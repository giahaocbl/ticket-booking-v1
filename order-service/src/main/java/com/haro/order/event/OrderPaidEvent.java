package com.haro.order.event;

import java.util.UUID;

public record OrderPaidEvent(
        UUID orderId,
        UUID userId
) {}
