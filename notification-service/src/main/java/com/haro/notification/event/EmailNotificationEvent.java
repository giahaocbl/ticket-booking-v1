package com.haro.notification.event;

import java.util.Map;
import java.util.UUID;

public record EmailNotificationEvent(
        String type,
        UUID userId,
        String email,
        String name,
        Map<String, String> payload
) {
}