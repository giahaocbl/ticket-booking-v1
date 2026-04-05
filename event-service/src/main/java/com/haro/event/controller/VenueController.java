package com.haro.event.controller;

import com.haro.event.dto.CreateVenueRequest;
import com.haro.event.dto.VenueDto;
import com.haro.event.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueDto> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        VenueDto venue = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(venue);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueDto> getVenueById(@PathVariable UUID id) {
        VenueDto venue = venueService.getVenueById(id);
        return ResponseEntity.ok(venue);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<VenueDto> getVenueBySlug(@PathVariable String slug) {
        VenueDto venue = venueService.getVenueBySlug(slug);
        return ResponseEntity.ok(venue);
    }

    @GetMapping
    public ResponseEntity<List<VenueDto>> getAllVenues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String city) {
        List<VenueDto> venues;
        if (status != null) {
            venues = venueService.getVenuesByStatus(status);
        } else if (countryCode != null && city != null) {
            venues = venueService.getVenuesByCountryAndCity(countryCode, city);
        } else {
            venues = venueService.getAllVenues();
        }
        return ResponseEntity.ok(venues);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueDto> updateVenue(
            @PathVariable UUID id,
            @Valid @RequestBody CreateVenueRequest request) {
        VenueDto venue = venueService.updateVenue(id, request);
        return ResponseEntity.ok(venue);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable UUID id) {
        venueService.deleteVenue(id);
        return ResponseEntity.noContent().build();
    }
}
