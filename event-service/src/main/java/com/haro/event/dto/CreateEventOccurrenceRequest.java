package com.haro.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CreateEventOccurrenceRequest {
    @NotNull(message = "Event ID is required")
    private UUID eventId;

    private UUID venueId;

    @NotNull(message = "Start time is required")
    private OffsetDateTime startsAt;

    private OffsetDateTime endsAt;

    private String timezone;

    private String status;
}
