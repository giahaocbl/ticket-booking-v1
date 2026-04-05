package com.haro.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_snapshots")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "eventOccurrenceId", nullable = false)
    private UUID eventOccurrenceId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(nullable = false)
    @Positive
    private int totalCapacity;

    private int reservedCount;

    private int confirmedCount;

    @Version
    private Integer version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        InventorySnapshot that = (InventorySnapshot) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Transient
    public int getAvailable() {
        return totalCapacity - reservedCount - confirmedCount;
    }
}
