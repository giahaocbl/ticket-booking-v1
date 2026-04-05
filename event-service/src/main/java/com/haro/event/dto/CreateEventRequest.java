package com.haro.event.dto;

import com.haro.event.entity.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CreateEventRequest {
    @NotNull(message = "Organizer ID is required")
    private UUID organizerId;

    private UUID venueId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Size(max = 512, message = "Cover image URL must not exceed 512 characters")
    private String coverImageUrl;

    private EventStatus status;

    private OffsetDateTime saleStartsAt;
    private OffsetDateTime saleEndsAt;
}
