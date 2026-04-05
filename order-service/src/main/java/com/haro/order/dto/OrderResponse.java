package com.haro.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        UUID reservationId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime paidAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items
) {
}
