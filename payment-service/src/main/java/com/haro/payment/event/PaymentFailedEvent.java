package com.haro.payment.event;


import java.util.UUID;

public record PaymentFailedEvent(
        String eventType,
        UUID orderId,
        UUID reservationId,
        String reason
) {
    public PaymentFailedEvent(UUID orderId, UUID reservationId, String reason) {
        this("PaymentFailed", orderId, reservationId, reason);
    }
}
