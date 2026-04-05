package com.haro.order.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID eventId,
        @NotNull UUID eventOccurrenceId,
        @NotNull UUID ticketTypeId,
        @NotBlank @Size(max = 500) String eventTitle,
        @NotNull OffsetDateTime occurrenceStartsAt,
        @NotBlank @Size(max = 255) String ticketTypeName,
        @Min(1) @Max(20) int quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        UUID reservationId
) {
}