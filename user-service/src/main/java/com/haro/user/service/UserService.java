package com.haro.user.service;

import com.haro.common.web.ConflictException;
import com.haro.common.web.NotFoundException;
import com.haro.user.dto.ChangePasswordRequest;
import com.haro.user.dto.UpdateProfileRequest;
import com.haro.user.dto.UserRegistrationRequest;
import com.haro.user.dto.UserResponse;
import com.haro.user.entity.User;
import com.haro.user.entity.UserRole;
import com.haro.user.repository.UserRepository;
import com.haro.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int UNVERIFIED_ACCOUNT_EXPIRY_HOURS = 24;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;


    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(request.email());

        if (existing.isPresent()) {
            User stale = existing.get();

            if (stale.getEmailVerifiedAt() != null) {
                throw new ConflictException("Email is already in use");
            }

            boolean withinGracePeriod = stale.getCreatedAt().plusHours(UNVERIFIED_ACCOUNT_EXPIRY_HOURS)
                    .isAfter(OffsetDateTime.now());

            if (withinGracePeriod) {
                throw new ConflictException(
                        "A verification email was already sent. Please check your inbox or try again later.");
            }

            log.info("Removing stale unverified account for email={}", stale.getEmail());
            userRepository.delete(stale);
            userRepository.flush();
        }


        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .status("pending_verification")
                .build();

        User saved = userRepository.save(user);

        UserRole role = UserRole.builder()
                .user(saved)
                .role("ROLE_USER")
                .build();
        userRoleRepository.save(role);

        emailVerificationService.createVerificationToken(saved);

        return toResponse(saved, Set.of("ROLE_USER"));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Set<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        return toResponse(user, roles);
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User saved = userRepository.save(user);
        Set<String> roles = userRoleRepository.findByUser(saved).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        return toResponse(saved, roles);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ConflictException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserResponse toResponse(User user, Set<String> roles) {
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
}
