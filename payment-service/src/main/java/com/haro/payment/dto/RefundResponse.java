package com.haro.payment.dto;

import com.haro.payment.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        String providerRefundId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
