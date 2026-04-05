package com.haro.payment.dto;

import com.haro.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String providerPaymentId,
        String failureCode,
        String failureMessage,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
