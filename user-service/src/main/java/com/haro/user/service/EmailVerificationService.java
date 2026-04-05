package com.haro.user.service;

import com.haro.common.web.BadRequestException;
import com.haro.common.web.ConflictException;
import com.haro.common.web.NotFoundException;
import com.haro.user.dto.ResendVerificationRequest;
import com.haro.user.dto.VerifyEmailRequest;
import com.haro.user.entity.EmailVerificationToken;
import com.haro.user.entity.User;
import com.haro.user.event.EmailNotificationEvent;
import com.haro.user.repository.EmailVerificationTokenRepository;
import com.haro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    @Transactional
    public EmailVerificationResult createVerificationToken(User user) {
        String rawToken = UUID.randomUUID().toString();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode(rawToken))
                .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                .build();
        token = tokenRepository.save(token);

        outboxService.saveEvent(new EmailNotificationEvent(
                "EMAIL_VERIFICATION",
                user.getId(),
                user.getEmail(),
                user.getName(),
                Map.of("tokenId", token.getId().toString(), "rawToken", rawToken)
        ));
        return new EmailVerificationResult(token.getId(), rawToken);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = tokenRepository.findById(request.tokenId())
                .orElseThrow(() -> new NotFoundException("Verification token not found"));

        if (token.getVerifiedAt() != null) {
            throw new ConflictException("Email already verified");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Verification token expired");
        }

        if (!passwordEncoder.matches(request.token(), token.getTokenHash())) {
            throw new BadRequestException("Invalid verification token");
        }

        token.setVerifiedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setStatus("active");
        userRepository.save(user);

        log.info("Email verified for user={}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(UUID tokenId, String rawToken) {
        EmailVerificationToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotFoundException("Verification token not found"));

        if (token.getVerifiedAt() != null) {
            throw new ConflictException("Email already verified");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Verification token expired");
        }

        if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
            throw new BadRequestException("Invalid verification token");
        }

        token.setVerifiedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerifiedAt(OffsetDateTime.now());
        user.setStatus("active");
        userRepository.save(user);

        log.info("Email verified for user={}", user.getEmail());
    }

    @Transactional
    public EmailVerificationResult resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getEmailVerifiedAt() != null) {
            throw new ConflictException("Email is already verified");
        }

        List<EmailVerificationToken> existingTokens = tokenRepository.findByUserAndVerifiedAtIsNullOrderByCreatedAtDesc(user);
        if (!existingTokens.isEmpty()) {
            EmailVerificationToken latest = existingTokens.getFirst();
            OffsetDateTime cooldownEnd = latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (cooldownEnd.isAfter(OffsetDateTime.now())) {
                throw new BadRequestException("Please wait before requesting another verification email");
            }
        }

        return createVerificationToken(user);
    }

    public record EmailVerificationResult(UUID tokenId, String rawToken) {
    }
}
