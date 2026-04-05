package com.haro.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededEvent(
        String eventType,
        UUID orderId,
        UUID paymentId,
        UUID reservationId,
        BigDecimal amount
) {
    public PaymentSucceededEvent(UUID orderId, UUID paymentId, UUID reservationId, BigDecimal amount) {
        this("PaymentSucceeded", orderId, paymentId, reservationId, amount);
    }
}
