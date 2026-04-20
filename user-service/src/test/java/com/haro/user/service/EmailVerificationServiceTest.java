package com.haro.user.service;

import com.haro.common.web.BadRequestException;
import com.haro.common.web.ConflictException;
import com.haro.user.dto.ResendVerificationRequest;
import com.haro.user.dto.VerifyEmailRequest;
import com.haro.user.entity.EmailVerificationToken;
import com.haro.user.entity.User;
import com.haro.user.repository.EmailVerificationTokenRepository;
import com.haro.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void verifyEmailWithRequestMarksTokenAndUserAsVerified() {
        UUID tokenId = UUID.randomUUID();
        User user = User.builder().email("user@test.com").status("pending").build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(tokenId)
                .user(user)
                .tokenHash("hashed")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("raw-token", "hashed")).thenReturn(true);

        emailVerificationService.verifyEmail(new VerifyEmailRequest(tokenId, "raw-token"));

        assertNotNull(token.getVerifiedAt());
        assertNotNull(user.getEmailVerifiedAt());
        assertEquals("active", user.getStatus());
        verify(tokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailWithRequestThrowsWhenTokenExpired() {
        UUID tokenId = UUID.randomUUID();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(tokenId)
                .user(User.builder().email("user@test.com").status("pending").build())
                .tokenHash("hashed")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> emailVerificationService.verifyEmail(new VerifyEmailRequest(tokenId, "raw-token"))
        );

        assertEquals("Verification token expired", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailWithParamsThrowsWhenTokenDoesNotMatch() {
        UUID tokenId = UUID.randomUUID();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(tokenId)
                .user(User.builder().email("user@test.com").status("pending").build())
                .tokenHash("hashed")
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .build();
        when(tokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("wrong-token", "hashed")).thenReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> emailVerificationService.verifyEmail(tokenId, "wrong-token")
        );

        assertEquals("Invalid verification token", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationThrowsWhenUserAlreadyVerified() {
        User user = User.builder()
                .email("verified@test.com")
                .emailVerifiedAt(OffsetDateTime.now().minusDays(1))
                .build();
        when(userRepository.findByEmailIgnoreCase("verified@test.com")).thenReturn(Optional.of(user));

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> emailVerificationService.resendVerification(new ResendVerificationRequest("verified@test.com"))
        );

        assertEquals("Email is already verified", ex.getMessage());
        verify(tokenRepository, never()).findByUserAndVerifiedAtIsNullOrderByCreatedAtDesc(any());
    }

    @Test
    void resendVerificationThrowsWhenCooldownNotElapsed() {
        User user = User.builder().email("user@test.com").build();
        EmailVerificationToken latestToken = EmailVerificationToken.builder()
                .user(user)
                .createdAt(OffsetDateTime.now().minusSeconds(30))
                .build();

        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByUserAndVerifiedAtIsNullOrderByCreatedAtDesc(user))
                .thenReturn(List.of(latestToken));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> emailVerificationService.resendVerification(new ResendVerificationRequest("user@test.com"))
        );

        assertEquals("Please wait before requesting another verification email", ex.getMessage());
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void createVerificationTokenSavesTokenAndPublishesOutboxEvent() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .name("User")
                .build();
        UUID generatedTokenId = UUID.randomUUID();
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> {
            EmailVerificationToken token = invocation.getArgument(0);
            token.setId(generatedTokenId);
            return token;
        });

        EmailVerificationService.EmailVerificationResult result = emailVerificationService.createVerificationToken(user);

        assertEquals(generatedTokenId, result.tokenId());
        assertNotNull(result.rawToken());
        assertFalse(result.rawToken().isBlank());

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals(user, tokenCaptor.getValue().getUser());
        assertEquals("hashed-token", tokenCaptor.getValue().getTokenHash());
        verify(outboxService).saveEvent(any());
    }
}
