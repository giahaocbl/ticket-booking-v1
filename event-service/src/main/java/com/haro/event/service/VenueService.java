package com.haro.event.service;


import com.haro.event.entity.Venue;
import com.haro.event.dto.CreateVenueRequest;
import com.haro.event.dto.VenueDto;
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
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueDto createVenue(CreateVenueRequest request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .slug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getName()))
                .description(request.getDescription())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .stateRegion(request.getStateRegion())
                .postalCode(request.getPostalCode())
                .countryCode(request.getCountryCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .capacity(request.getCapacity())
                .status("active")
                .build();

        venue = venueRepository.save(venue);
        return mapToDto(venue);
    }

    @Transactional(readOnly = true)
    public VenueDto getVenueById(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found with id: " + id));
        return mapToDto(venue);
    }

    @Transactional(readOnly = true)
    public VenueDto getVenueBySlug(String slug) {
        Venue venue = venueRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Venue not found with slug: " + slug));
        return mapToDto(venue);
    }

    @Transactional(readOnly = true)
    public List<VenueDto> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VenueDto> getVenuesByStatus(String status) {
        return venueRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VenueDto> getVenuesByCountryAndCity(String countryCode, String city) {
        return venueRepository.findByCountryAndCity(countryCode, city).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public VenueDto updateVenue(UUID id, CreateVenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found with id: " + id));

        venue.setName(request.getName());
        if (request.getSlug() != null) {
            venue.setSlug(request.getSlug());
        }
        venue.setDescription(request.getDescription());
        venue.setAddressLine1(request.getAddressLine1());
        venue.setAddressLine2(request.getAddressLine2());
        venue.setCity(request.getCity());
        venue.setStateRegion(request.getStateRegion());
        venue.setPostalCode(request.getPostalCode());
        venue.setCountryCode(request.getCountryCode());
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setTimezone(request.getTimezone());
        venue.setCapacity(request.getCapacity());

        venue = venueRepository.save(venue);
        return mapToDto(venue);
    }

    public void deleteVenue(UUID id) {
        if (!venueRepository.existsById(id)) {
            throw new RuntimeException("Venue not found with id: " + id);
        }
        venueRepository.deleteById(id);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private VenueDto mapToDto(Venue venue) {
        return VenueDto.builder()
                .id(venue.getId())
                .name(venue.getName())
                .slug(venue.getSlug())
                .description(venue.getDescription())
                .addressLine1(venue.getAddressLine1())
                .addressLine2(venue.getAddressLine2())
                .city(venue.getCity())
                .stateRegion(venue.getStateRegion())
                .postalCode(venue.getPostalCode())
                .countryCode(venue.getCountryCode())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .timezone(venue.getTimezone())
                .capacity(venue.getCapacity())
                .status(venue.getStatus())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}