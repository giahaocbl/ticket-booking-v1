package com.haro.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PasswordResetRequest(@Email @NotBlank String email) {
}
