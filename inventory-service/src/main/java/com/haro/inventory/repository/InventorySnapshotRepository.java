package com.haro.inventory.repository;

import com.haro.inventory.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {
    @Query("""
    SELECT i
    FROM inventory_snapshot i
    WHERE event_occurrence_uuid = :eventOccurrenceId
    AND ticket_type_id = :ticketTypeId
    """)
    Optional<InventorySnapshot> getAvailability(UUID eventOccurrenceId, UUID ticketTypeId);

    Optional<InventorySnapshot> findByEventOccurrenceIdAndTicketTypeId(UUID eventOccurrenceId, UUID ticketTypeId);
}
