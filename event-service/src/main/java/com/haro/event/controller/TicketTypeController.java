package com.haro.event.controller;

import com.haro.event.dto.CreateTicketTypeRequest;
import com.haro.event.dto.TicketTypeDto;
import com.haro.event.service.TicketTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @PostMapping
    public ResponseEntity<TicketTypeDto> createTicketType(
            @Valid @RequestBody CreateTicketTypeRequest request) {
        TicketTypeDto ticketType = ticketTypeService.createTicketType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketType);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketTypeDto> getTicketTypeById(@PathVariable UUID id) {
        TicketTypeDto ticketType = ticketTypeService.getTicketTypeById(id);
        return ResponseEntity.ok(ticketType);
    }

    @GetMapping
    public ResponseEntity<List<TicketTypeDto>> getTicketTypes(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        List<TicketTypeDto> ticketTypes;
        if (eventId != null && activeOnly) {
            ticketTypes = ticketTypeService.getActiveTicketTypesByEventId(eventId);
        } else if (eventId != null) {
            ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        } else {
            ticketTypes = ticketTypeService.getAllTicketTypes();
        }
        return ResponseEntity.ok(ticketTypes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketTypeDto> updateTicketType(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTicketTypeRequest request) {
        TicketTypeDto ticketType = ticketTypeService.updateTicketType(id, request);
        return ResponseEntity.ok(ticketType);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicketType(@PathVariable UUID id) {
        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.noContent().build();
    }
}
