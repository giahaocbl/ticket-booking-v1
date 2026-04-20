package com.haro.inventory.waitingroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WaitingRoomConfigRequest(
        @NotNull @Min(1) Integer capacity,
        @Min(10) Long admissionPassTtlSeconds,
        Boolean enabled
) {
}
