package com.haro.order.event;

import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        UUID userId,
        String reason
) {}