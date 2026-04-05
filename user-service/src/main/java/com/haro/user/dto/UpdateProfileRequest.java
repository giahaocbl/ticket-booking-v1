package com.haro.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String name,
        @Size(max = 50) String phone,
        @Size(max = 512) String avatarUrl
) {
}