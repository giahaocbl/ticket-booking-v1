package com.haro.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyEmailRequest(@NotNull UUID tokenId,
                                 @NotBlank String token) {
}
