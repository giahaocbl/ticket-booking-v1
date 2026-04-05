package com.haro.user.service;

import com.haro.common.web.BadRequestException;
import com.haro.common.web.NotFoundException;
import com.haro.user.config.JwtProperties;
import com.haro.user.dto.AuthResponse;
import com.haro.user.dto.LoginRequest;
import com.haro.user.dto.RefreshTokenRequest;
import com.haro.user.dto.UserResponse;
import com.haro.user.entity.Session;
import com.haro.user.entity.User;
import com.haro.user.entity.UserRole;
import com.haro.user.repository.SessionRepository;
import com.haro.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int REFRESH_TOKEN_DAYS = 30;

    private final UserService userService;
    //    private final UserRoleRepository userRoleRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmailOrThrow(request.email());

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        Set<String> roles = loadRoles(user);
        return createSession(user, roles);
    }


    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Session not found"));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            sessionRepository.delete(session);
            throw new BadRequestException("Refresh token expired");
        }

        if (!passwordEncoder.matches(request.refreshToken(), session.getRefreshTokenHash())) {
            sessionRepository.delete(session);
            throw new BadRequestException("Invalid refresh token");
        }

        String newRefreshToken = UUID.randomUUID().toString();
        session.setRefreshTokenHash(passwordEncoder.encode(newRefreshToken));
        session.setExpiresAt(refreshExpiresAt());
        sessionRepository.save(session);

        User user = session.getUser();
        Set<String> roles = loadRoles(user);
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                session.getId(),
                toUserResponse(user, roles)
        );
    }

    @Transactional
    public void logout(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    private Set<String> loadRoles(User user) {
        return userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
    }

    private UserResponse toUserResponse(User user, Set<String> roles) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roles
        );
    }

    private OffsetDateTime refreshExpiresAt() {
        long refreshMs = jwtProperties.refreshTokenExpirationMs();
        return OffsetDateTime.now().plus(refreshMs, ChronoUnit.MILLIS);
    }

    private AuthResponse createSession(User user, Set<String> roles) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshToken = UUID.randomUUID().toString();

        Session session = Session.builder()
                .user(user)
                .refreshTokenHash(passwordEncoder.encode(refreshToken))
                .expiresAt(refreshExpiresAt())
                .build();
        session = sessionRepository.save(session);

        return new AuthResponse(
                accessToken,
                refreshToken,
                session.getId(),
                toUserResponse(user, roles)
        );
    }
}
