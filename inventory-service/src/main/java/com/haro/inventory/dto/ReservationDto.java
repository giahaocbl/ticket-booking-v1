package com.haro.inventory.dto;

import com.haro.inventory.entity.ReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;


public record ReservationDto(UUID id, UUID eventOccurrenceId, UUID ticketTypeId,
                             UUID userId, int quantity,
                             ReservationStatus status,
                             OffsetDateTime expiresAt, OffsetDateTime createdAt,
                             OffsetDateTime updateAt) {
}
