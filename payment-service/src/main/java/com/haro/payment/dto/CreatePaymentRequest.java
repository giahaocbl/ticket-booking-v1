package com.haro.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 50) String provider,
        @NotBlank @Size(max = 255) String idempotencyKey
) {
}