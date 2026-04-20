package com.haro.user.controller;

import com.haro.user.dto.*;
import com.haro.user.service.AuthService;
import com.haro.user.service.EmailVerificationService;
import com.haro.user.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginReturnsOkAndAuthResponse() {
        LoginRequest request = new LoginRequest("user@test.com", "secret123");
        AuthResponse expected = new AuthResponse(
                "access-token",
                "refresh-token",
                UUID.randomUUID(),
                new UserResponse(UUID.randomUUID(), "user@test.com", "User", null, null, "active", null, null, null, Set.of("USER"))
        );
        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(authService).login(request);
    }

    @Test
    void refreshReturnsOkAndAuthResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest(UUID.randomUUID(), "refresh-token");
        AuthResponse expected = new AuthResponse("new-access", "new-refresh", UUID.randomUUID(), null);
        when(authService.refresh(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = authController.refresh(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(authService).refresh(request);
    }

    @Test
    void logoutReturnsNoContent() {
        UUID sessionId = UUID.randomUUID();
        LogoutRequest request = new LogoutRequest(sessionId);

        ResponseEntity<Void> response = authController.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(authService).logout(sessionId);
    }

    @Test
    void createPasswordResetReturnsTokenPayload() {
        PasswordResetRequest request = new PasswordResetRequest("user@test.com");
        PasswordResetResponse expected = new PasswordResetResponse(UUID.randomUUID(), "raw-token");
        when(passwordResetService.createResetToken(request)).thenReturn(expected);

        ResponseEntity<PasswordResetResponse> response = authController.createPasswordReset(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(passwordResetService).createResetToken(request);
    }

    @Test
    void confirmPasswordResetReturnsNoContent() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(UUID.randomUUID(), "token", "newPassword123");

        ResponseEntity<Void> response = authController.confirmPasswordReset(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(passwordResetService).resetPassword(request);
    }

    @Test
    void verifyEmailViaBodyReturnsSuccessMessage() {
        VerifyEmailRequest request = new VerifyEmailRequest(UUID.randomUUID(), "token");

        ResponseEntity<Map<String, String>> response = authController.verifyEmail(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email verified successfully", response.getBody().get("message"));
        verify(emailVerificationService).verifyEmail(request);
    }

    @Test
    void verifyEmailViaQueryReturnsSuccessMessage() {
        UUID tokenId = UUID.randomUUID();
        String token = "raw-token";

        ResponseEntity<Map<String, String>> response = authController.verifyEmail(tokenId, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Email verified successfully", response.getBody().get("message"));
        verify(emailVerificationService).verifyEmail(tokenId, token);
    }

    @Test
    void resendVerificationReturnsDevPayload() {
        ResendVerificationRequest request = new ResendVerificationRequest("user@test.com");
        UUID tokenId = UUID.randomUUID();
        String rawToken = "token-123";
        when(emailVerificationService.resendVerification(request))
                .thenReturn(new EmailVerificationService.EmailVerificationResult(tokenId, rawToken));

        ResponseEntity<Map<String, Object>> response = authController.resendVerification(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Verification email sent", response.getBody().get("message"));
        assertEquals(tokenId.toString(), response.getBody().get("tokenId"));
        assertEquals(rawToken, response.getBody().get("token"));
        verify(emailVerificationService).resendVerification(request);
    }
}
