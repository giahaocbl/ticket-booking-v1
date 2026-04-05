package com.haro.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id"),
                @Index(name = "idx_order_items_event_id", columnList = "event_id"),
                @Index(name = "idx_order_items_occurrence", columnList = "event_occurrence_id")
        }
)@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_occurrence_id", nullable = false)
    private UUID eventOccurrenceId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "event_title", nullable = false, length = 500)
    private String eventTitle;

    @Column(name = "occurrence_starts_at", nullable = false)
    private OffsetDateTime occurrenceStartsAt;

    @Column(name = "ticket_type_name", nullable = false, length = 255)
    private String ticketTypeName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "reservation_id")
    private UUID reservationId;
}
