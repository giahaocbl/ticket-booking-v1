package com.haro.inventory.service;

import com.haro.inventory.entity.InventorySnapshot;
import com.haro.inventory.dto.AvailabilityResponse;
import com.haro.inventory.repository.InventorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventorySnapshotRepository inventorySnapshotRepository;

    public AvailabilityResponse getAvailability(UUID eventOccurrenceId, UUID ticketTypeId) {
        return mapToDto(inventorySnapshotRepository.getAvailability(eventOccurrenceId, ticketTypeId)
                .orElseThrow(() -> new RuntimeException("Inventory not found!")));
    }

    private AvailabilityResponse mapToDto(InventorySnapshot inventorySnapshot) {
        return new AvailabilityResponse(inventorySnapshot.getEventOccurrenceId() , inventorySnapshot.getTicketTypeId(), inventorySnapshot.getTotalCapacity(),
                inventorySnapshot.getReservedCount(), inventorySnapshot.getConfirmedCount(), inventorySnapshot.getAvailable());
    }
}
