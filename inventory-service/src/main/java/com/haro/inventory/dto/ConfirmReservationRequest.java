package com.haro.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record ConfirmReservationRequest(@NotNull UUID orderId) {
}
