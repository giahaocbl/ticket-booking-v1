package com.haro.event.service;

import com.haro.event.entity.Event;
import com.haro.event.entity.EventOccurrence;
import com.haro.event.entity.Venue;
import com.haro.event.dto.CreateEventOccurrenceRequest;
import com.haro.event.dto.EventOccurrenceDto;
import com.haro.event.repository.EventOccurrenceRepository;
import com.haro.event.repository.EventRepository;
import com.haro.event.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventOccurrenceService {

    private final EventOccurrenceRepository occurrenceRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventOccurrenceDto createOccurrence(CreateEventOccurrenceRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + request.getEventId()));

        EventOccurrence occurrence = EventOccurrence.builder()
                .event(event)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .timezone(request.getTimezone())
                .status(request.getStatus() != null ? request.getStatus() : "scheduled")
                .build();

        // Use venue from request or fall back to event's venue
        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));
            occurrence.setVenue(venue);
        } else if (event.getVenue() != null) {
            occurrence.setVenue(event.getVenue());
        }

        occurrence = occurrenceRepository.save(occurrence);
        return mapToDto(occurrence);
    }

    @Transactional(readOnly = true)
    public EventOccurrenceDto getOccurrenceById(UUID id) {
        EventOccurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event occurrence not found with id: " + id));
        return mapToDto(occurrence);
    }

    @Transactional(readOnly = true)
    public List<EventOccurrenceDto> getOccurrencesByEventId(UUID eventId) {
        return occurrenceRepository.findByEventId(eventId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventOccurrenceDto> getOccurrencesByStatus(String status) {
        return occurrenceRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventOccurrenceDto> getAllOccurrences() {
        return occurrenceRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public EventOccurrenceDto updateOccurrence(UUID id, CreateEventOccurrenceRequest request) {
        EventOccurrence occurrence = occurrenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event occurrence not found with id: " + id));

        occurrence.setStartsAt(request.getStartsAt());
        occurrence.setEndsAt(request.getEndsAt());
        occurrence.setTimezone(request.getTimezone());
        if (request.getStatus() != null) {
            occurrence.setStatus(request.getStatus());
        }

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));
            occurrence.setVenue(venue);
        }

        occurrence = occurrenceRepository.save(occurrence);
        return mapToDto(occurrence);
    }

    public void deleteOccurrence(UUID id) {
        if (!occurrenceRepository.existsById(id)) {
            throw new RuntimeException("Event occurrence not found with id: " + id);
        }
        occurrenceRepository.deleteById(id);
    }

    private EventOccurrenceDto mapToDto(EventOccurrence occurrence) {
        return EventOccurrenceDto.builder()
                .id(occurrence.getId())
                .eventId(occurrence.getEvent().getId())
                .venueId(occurrence.getVenue() != null ? occurrence.getVenue().getId() : null)
                .startsAt(occurrence.getStartsAt())
                .endsAt(occurrence.getEndsAt())
                .timezone(occurrence.getTimezone())
                .status(occurrence.getStatus())
                .createdAt(occurrence.getCreatedAt())
                .updatedAt(occurrence.getUpdatedAt())
                .build();
    }
}

