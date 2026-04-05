package com.haro.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventOccurrenceDto {
    private UUID id;
    private UUID eventId;
    private UUID venueId;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String timezone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
