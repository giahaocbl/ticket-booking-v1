package com.haro.event.controller;

import com.haro.event.dto.CreateEventOccurrenceRequest;
import com.haro.event.dto.EventOccurrenceDto;
import com.haro.event.service.EventOccurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/event-occurrences")
@RequiredArgsConstructor
public class EventOccurrenceController {

    private final EventOccurrenceService occurrenceService;

    @PostMapping
    public ResponseEntity<EventOccurrenceDto> createOccurrence(
            @Valid @RequestBody CreateEventOccurrenceRequest request) {
        EventOccurrenceDto occurrence = occurrenceService.createOccurrence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(occurrence);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventOccurrenceDto> getOccurrenceById(@PathVariable UUID id) {
        EventOccurrenceDto occurrence = occurrenceService.getOccurrenceById(id);
        return ResponseEntity.ok(occurrence);
    }

    @GetMapping
    public ResponseEntity<List<EventOccurrenceDto>> getOccurrences(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) String status) {
        List<EventOccurrenceDto> occurrences;
        if (eventId != null) {
            occurrences = occurrenceService.getOccurrencesByEventId(eventId);
        } else if (status != null) {
            occurrences = occurrenceService.getOccurrencesByStatus(status);
        } else {
            occurrences = occurrenceService.getAllOccurrences();
        }
        return ResponseEntity.ok(occurrences);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventOccurrenceDto> updateOccurrence(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEventOccurrenceRequest request) {
        EventOccurrenceDto occurrence = occurrenceService.updateOccurrence(id, request);
        return ResponseEntity.ok(occurrence);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOccurrence(@PathVariable UUID id) {
        occurrenceService.deleteOccurrence(id);
        return ResponseEntity.noContent().build();
    }
}
