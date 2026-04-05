package com.haro.event.service;

import com.haro.event.entity.Event;
import com.haro.event.entity.TicketType;
import com.haro.event.dto.CreateTicketTypeRequest;
import com.haro.event.dto.TicketTypeDto;
import com.haro.event.repository.EventRepository;
import com.haro.event.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;

    public TicketTypeDto createTicketType(CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + request.getEventId()));

        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(request.getName())
                .description(request.getDescription())
                .priceAmount(request.getPriceAmount())
                .priceCurrency(request.getPriceCurrency())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ticketType = ticketTypeRepository.save(ticketType);
        return mapToDto(ticketType);
    }

    @Transactional(readOnly = true)
    public TicketTypeDto getTicketTypeById(UUID id) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket type not found with id: " + id));
        return mapToDto(ticketType);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDto> getTicketTypesByEventId(UUID eventId) {
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDto> getActiveTicketTypesByEventId(UUID eventId) {
        return ticketTypeRepository.findActiveByEventIdOrderBySortOrder(eventId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDto> getAllTicketTypes() {
        return ticketTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TicketTypeDto updateTicketType(UUID id, CreateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket type not found with id: " + id));

        ticketType.setName(request.getName());
        ticketType.setDescription(request.getDescription());
        ticketType.setPriceAmount(request.getPriceAmount());
        ticketType.setPriceCurrency(request.getPriceCurrency());
        if (request.getSortOrder() != null) {
            ticketType.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            ticketType.setIsActive(request.getIsActive());
        }

        ticketType = ticketTypeRepository.save(ticketType);
        return mapToDto(ticketType);
    }

    public void deleteTicketType(UUID id) {
        if (!ticketTypeRepository.existsById(id)) {
            throw new RuntimeException("Ticket type not found with id: " + id);
        }
        ticketTypeRepository.deleteById(id);
    }

    private TicketTypeDto mapToDto(TicketType ticketType) {
        return TicketTypeDto.builder()
                .id(ticketType.getId())
                .eventId(ticketType.getEvent().getId())
                .name(ticketType.getName())
                .description(ticketType.getDescription())
                .priceAmount(ticketType.getPriceAmount())
                .priceCurrency(ticketType.getPriceCurrency())
                .sortOrder(ticketType.getSortOrder())
                .isActive(ticketType.getIsActive())
                .createdAt(ticketType.getCreatedAt())
                .updatedAt(ticketType.getUpdatedAt())
                .build();
    }
}
