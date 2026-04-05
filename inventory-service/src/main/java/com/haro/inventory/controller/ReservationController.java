package com.haro.inventory.controller;

import com.haro.inventory.entity.ReservationStatus;
import com.haro.inventory.dto.ConfirmReservationRequest;
import com.haro.inventory.dto.CreateReservationRequest;
import com.haro.inventory.dto.ReservationDto;
import com.haro.inventory.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/")
    public ResponseEntity<ReservationDto> createReservation(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ReservationDto> confirmReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmReservationRequest request
    ) {
        ReservationDto reservation = reservationService.confirmReservation(id, request);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationDto> cancelReservation(@PathVariable UUID id) {
        ReservationDto reservation = reservationService.cancelReservation(id);
        return ResponseEntity.ok(reservation);
    }

    @GetMapping
    public ResponseEntity<Page<ReservationDto>> getReservationsForUser(
            @RequestParam UUID userId,
            @RequestParam(required = false) ReservationStatus status,
            Pageable pageable
    ) {
        Page<ReservationDto> reservations = reservationService.getReservationsForUser(userId, status, pageable);
        return ResponseEntity.ok(reservations);
    }
}
