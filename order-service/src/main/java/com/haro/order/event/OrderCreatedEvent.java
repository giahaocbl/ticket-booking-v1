package com.haro.order.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        UUID reservationId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {}