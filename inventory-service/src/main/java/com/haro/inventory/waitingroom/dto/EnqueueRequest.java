package com.haro.inventory.waitingroom.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnqueueRequest(@NotNull UUID userId) {
}
