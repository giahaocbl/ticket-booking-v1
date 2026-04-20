package com.haro.inventory.waitingroom.dto;

public record EnqueueResponse(
        String token,
        long position,
        WaitingRoomStatus status,
        boolean existing
) {
}
