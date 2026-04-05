package com.haro.inventory.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateReservationRequest(
        @NotNull(message = "Event Occurrence ID is required") UUID eventOccurrenceId,
        @NotNull(message = "Ticket Type ID is required") UUID ticketTypeId,
        @NotNull(message = "User ID is required") UUID userId, @Min(1) @Max(50) int quantity, @Min(1) @Max(900) int ttlSeconds,
        @NotBlank(message = "Idempotency Key required") @Size(max = 255) String idempotencyKey) {
}
