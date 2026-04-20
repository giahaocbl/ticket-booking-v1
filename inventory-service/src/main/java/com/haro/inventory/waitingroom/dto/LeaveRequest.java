package com.haro.inventory.waitingroom.dto;

import jakarta.validation.constraints.NotBlank;

public record LeaveRequest(@NotBlank String token) {
}
