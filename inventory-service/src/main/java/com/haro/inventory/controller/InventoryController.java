package com.haro.inventory.controller;

import com.haro.inventory.dto.AvailabilityResponse;
import com.haro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> getAvailable(@RequestParam UUID eventOccurrenceId,
                                                             @RequestParam UUID ticketTypeId) {
        return ResponseEntity.ok(inventoryService.getAvailability(eventOccurrenceId, ticketTypeId));
    }


}
