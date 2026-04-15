package com.haro.inventory.repository;

import com.haro.inventory.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {
    @Query("""
    SELECT i
    FROM InventorySnapshot i
    WHERE i.eventOccurrenceId = :eventOccurrenceId
    AND i.ticketTypeId = :ticketTypeId
    """)
    Optional<InventorySnapshot> getAvailability(UUID eventOccurrenceId, UUID ticketTypeId);

    Optional<InventorySnapshot> findByEventOccurrenceIdAndTicketTypeId(UUID eventOccurrenceId, UUID ticketTypeId);
}
