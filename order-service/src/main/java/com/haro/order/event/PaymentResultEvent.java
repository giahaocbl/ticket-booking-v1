package com.haro.order.event;


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
