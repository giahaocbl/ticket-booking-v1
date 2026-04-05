package com.haro.event.dto;

import com.haro.event.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private UUID id;
    private UUID organizerId;
    private UUID venueId;
    private VenueDto venue;
    private String title;
    private String slug;
    private String description;
    private String category;
    private String coverImageUrl;
    private EventStatus status;
    private OffsetDateTime saleStartsAt;
    private OffsetDateTime saleEndsAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<EventOccurrenceDto> occurrences;
    private List<TicketTypeDto> ticketTypes;
}
