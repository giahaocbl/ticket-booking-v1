package com.haro.event.service;

import com.haro.event.entity.Event;
import com.haro.event.entity.EventStatus;
import com.haro.event.entity.Venue;
import com.haro.event.dto.CreateEventRequest;
import com.haro.event.dto.EventDto;
import com.haro.event.dto.EventOccurrenceDto;
import com.haro.event.dto.TicketTypeDto;
import com.haro.event.repository.EventRepository;
import com.haro.event.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventDto createEvent(CreateEventRequest request) {
        Event event = Event.builder()
                .organizerId(request.getOrganizerId())
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .category(request.getCategory())
                .coverImageUrl(request.getCoverImageUrl())
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.SCHEDULED)
                .saleStartsAt(request.getSaleStartsAt())
                .saleEndsAt(request.getSaleEndsAt())
                .build();

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));
            event.setVenue(venue);
        }

        // Check for duplicate slug for the same organizer
        eventRepository.findByOrganizerIdAndSlug(request.getOrganizerId(), request.getSlug())
                .ifPresent(e -> {
                    throw new RuntimeException("Event with slug '" + request.getSlug() + "' already exists for this organizer");
                });

        event = eventRepository.save(event);
        return mapToDto(event);
    }

    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return mapToDto(event);
    }

    @Transactional(readOnly = true)
    public EventDto getEventByOrganizerIdAndSlug(UUID organizerId, String slug) {
        Event event = eventRepository.findByOrganizerIdAndSlug(organizerId, slug)
                .orElseThrow(() -> new RuntimeException("Event not found with organizerId: " + organizerId + " and slug: " + slug));
        return mapToDto(event);
    }

    @Transactional(readOnly = true)
    public List<EventDto> getEventsByOrganizer(UUID organizerId) {
        return eventRepository.findByOrganizerId(organizerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getEventsByStatus(String status) {
        return eventRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getEventsByCategory(String category) {
        return eventRepository.findByCategory(category).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public EventDto updateEvent(UUID id, CreateEventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        event.setTitle(request.getTitle());
        event.setSlug(request.getSlug());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        event.setSaleStartsAt(request.getSaleStartsAt());
        event.setSaleEndsAt(request.getSaleEndsAt());

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));
            event.setVenue(venue);
        }

        event = eventRepository.save(event);
        return mapToDto(event);
    }

    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    private EventDto mapToDto(Event event) {
        EventDto.EventDtoBuilder builder = EventDto.builder()
                .id(event.getId())
                .organizerId(event.getOrganizerId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .description(event.getDescription())
                .category(event.getCategory())
                .coverImageUrl(event.getCoverImageUrl())
                .status(event.getStatus())
                .saleStartsAt(event.getSaleStartsAt())
                .saleEndsAt(event.getSaleEndsAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt());

        if (event.getVenue() != null) {
            builder.venueId(event.getVenue().getId());
        }

        if (event.getOccurrences() != null && !event.getOccurrences().isEmpty()) {
            builder.occurrences(event.getOccurrences().stream()
                    .map(occ -> EventOccurrenceDto.builder()
                            .id(occ.getId())
                            .eventId(occ.getEvent().getId())
                            .venueId(occ.getVenue() != null ? occ.getVenue().getId() : null)
                            .startsAt(occ.getStartsAt())
                            .endsAt(occ.getEndsAt())
                            .timezone(occ.getTimezone())
                            .status(occ.getStatus())
                            .createdAt(occ.getCreatedAt())
                            .updatedAt(occ.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (event.getTicketTypes() != null && !event.getTicketTypes().isEmpty()) {
            builder.ticketTypes(event.getTicketTypes().stream()
                    .map(tt -> TicketTypeDto.builder()
                            .id(tt.getId())
                            .eventId(tt.getEvent().getId())
                            .name(tt.getName())
                            .description(tt.getDescription())
                            .priceAmount(tt.getPriceAmount())
                            .priceCurrency(tt.getPriceCurrency())
                            .sortOrder(tt.getSortOrder())
                            .isActive(tt.getIsActive())
                            .createdAt(tt.getCreatedAt())
                            .updatedAt(tt.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public Page<EventDto> searchEvents(UUID organizerId, String status, String category, Pageable pageable) {
        Pageable effectivePageable = clampPageable(pageable);
        return eventRepository.findByFilters(organizerId, status, category, effectivePageable)
                .map(this::mapToDto);
    }

    private Pageable clampPageable(Pageable pageable) {
        int maxPageSize = 200;
        int size = Math.min(pageable.getPageSize(), maxPageSize);

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        if (size == pageable.getPageSize() && sort.equals(pageable.getSort())) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
