package com.haro.user.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String phone,
        String avatarUrl,
        String status,
        OffsetDateTime emailVerifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Set<String> roles
) {
}