package com.haro.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefreshTokenRequest(
        @NotNull UUID sessionId,
        @NotNull String refreshToken
) {
}