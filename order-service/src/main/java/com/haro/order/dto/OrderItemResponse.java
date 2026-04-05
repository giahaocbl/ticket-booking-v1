package com.haro.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID eventId,
        UUID eventOccurrenceId,
        UUID ticketTypeId,
        String eventTitle,
        OffsetDateTime occurrenceStartsAt,
        String ticketTypeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        UUID reservationId
) {
}
