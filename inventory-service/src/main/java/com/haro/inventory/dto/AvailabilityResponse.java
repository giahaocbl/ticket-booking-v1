package com.haro.inventory.dto;

import java.util.UUID;


public record AvailabilityResponse(UUID eventOccurrenceId, UUID ticketTypeId, int totalCapacity,
                                   int reservedCount, int confirmedCount, int available) {
}
