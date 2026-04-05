package com.haro.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateTicketTypeRequest {
    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Price amount is required")
    @Positive(message = "Price amount must be positive")
    private BigDecimal priceAmount;

    @NotBlank(message = "Price currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String priceCurrency;

    private Integer sortOrder;

    private Boolean isActive;
}
