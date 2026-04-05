package com.haro.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateRegion;
    private String postalCode;
    private String countryCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private Integer capacity;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
