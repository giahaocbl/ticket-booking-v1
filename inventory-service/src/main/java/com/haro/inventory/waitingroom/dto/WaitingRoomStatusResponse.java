package com.haro.inventory.waitingroom.dto;

public record WaitingRoomStatusResponse(
        String token,
        WaitingRoomStatus status,
        Long position,
        Long expiresAt
) {
}
