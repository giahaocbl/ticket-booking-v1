package com.haro.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PasswordResetConfirmRequest(@NotBlank UUID tokenId,
                                          @NotBlank String token,
                                          @NotBlank @Size(min = 8, max = 100) String newPassword) {
}
