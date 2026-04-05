package com.haro.inventory.event;


import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResultEvent(
        String eventType,
        UUID orderId,
        UUID paymentId,
        UUID reservationId,
        BigDecimal amount,
        String reason
) {}
