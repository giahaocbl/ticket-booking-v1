package com.haro.user.service;

import com.haro.common.web.BadRequestException;
import com.haro.common.web.NotFoundException;
import com.haro.user.dto.PasswordResetConfirmRequest;
import com.haro.user.dto.PasswordResetRequest;
import com.haro.user.dto.PasswordResetResponse;
import com.haro.user.entity.PasswordResetToken;
import com.haro.user.entity.User;
import com.haro.user.repository.PasswordResetTokenRepository;
import com.haro.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int RESET_TOKEN_MINUTES = 10;

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public PasswordResetResponse createResetToken(@Valid PasswordResetRequest request) {
        User user = userService.findByEmailOrThrow(request.email());

        String resetToken = UUID.randomUUID().toString();
        String resetTokenHash = passwordEncoder.encode(resetToken);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(resetTokenHash)
                .expiresAt(OffsetDateTime.now().plusMinutes(RESET_TOKEN_MINUTES))
                .build();

        // TODO: check if save works or have to use saveAndFlush
        passwordResetTokenRepository.save(passwordResetToken);
        return new PasswordResetResponse(passwordResetToken.getId(), resetToken);
    }

    @Transactional
    public void resetPassword(@Valid PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findById(request.tokenId())
                .orElseThrow(() -> new NotFoundException("Invalid reset token"));

        if (token.getUsedAt() != null) {
            throw new BadRequestException("Token has already been used");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Expired reset token");
        }

        if (!passwordEncoder.matches(request.token(), token.getTokenHash())) {
            throw new BadRequestException("Invalid reset token");
        }

        User user = token.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(token);
    }
}
