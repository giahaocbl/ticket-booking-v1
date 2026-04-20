package com.haro.inventory.waitingroom.dto;

public record WaitingRoomConfigResponse(
        int capacity,
        long admissionPassTtlSeconds,
        long waitingTokenTtlSeconds,
        long queueDepth,
        long activeCount,
        boolean enabled
) {
}
