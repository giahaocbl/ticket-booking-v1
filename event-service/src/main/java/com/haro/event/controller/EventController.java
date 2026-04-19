package com.haro.event.controller;

import com.haro.event.dto.CreateEventRequest;
import com.haro.event.dto.EventDto;
import com.haro.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventDto event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
        EventDto event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/organizer/{organizerId}/slug/{slug}")
    public ResponseEntity<EventDto> getEventByOrganizerAndSlug(
            @PathVariable UUID organizerId,
            @PathVariable String slug) {
        EventDto event = eventService.getEventByOrganizerIdAndSlug(organizerId, slug);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<Page<EventDto>> getEvents(
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        Page<EventDto> events = eventService.searchEvents(organizerId, status, category, pageable);
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEventRequest request) {
        EventDto event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
