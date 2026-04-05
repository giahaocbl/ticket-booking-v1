package com.haro.inventory.service;

import com.haro.common.web.ConflictException;
import com.haro.common.web.NotFoundException;
import com.haro.inventory.entity.InventorySnapshot;
import com.haro.inventory.entity.Reservation;
import com.haro.inventory.entity.ReservationStatus;
import com.haro.inventory.dto.ConfirmReservationRequest;
import com.haro.inventory.dto.CreateReservationRequest;
import com.haro.inventory.repository.InventorySnapshotRepository;
import com.haro.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.haro.inventory.entity.ReservationStatus.EXPIRED;
import static com.haro.inventory.entity.ReservationStatus.PENDING;

@Service
@RequiredArgsConstructor
public class ReservationTxService {
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreationResult createReservationInNewTx(CreateReservationRequest request) {
        Reservation existing = reservationRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return new CreationResult(existing, false);
        }
        InventorySnapshot inventorySnapshot = inventorySnapshotRepository.findByEventOccurrenceIdAndTicketTypeId(request.eventOccurrenceId(), request.ticketTypeId())
                .orElseThrow(() -> new NotFoundException("Inventory snapshot not found!"));

        int available = inventorySnapshot.getAvailable();

        if (request.quantity() > available) {
            throw new ConflictException("Not enough capacity!");
        }

        inventorySnapshot.setReservedCount(inventorySnapshot.getReservedCount() + request.quantity());
        inventorySnapshotRepository.save(inventorySnapshot);

        Reservation newReservation = Reservation.builder()
                .eventOccurrenceId(request.eventOccurrenceId())
                .ticketTypeId(request.ticketTypeId())
                .userId(request.userId())
                .quantity(request.quantity())
                .status(PENDING)
                .expiresAt(OffsetDateTime.now().plusSeconds(request.ttlSeconds()))
                .idempotencyKey(request.idempotencyKey())
                .build();

        newReservation = reservationRepository.save(newReservation);
//        reservationTtlService.scheduleExpiration(newReservation.getId(), request.ttlSeconds());
        return new CreationResult(newReservation, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation confirmReservationInNewTx(UUID id, ConfirmReservationRequest request) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found!"));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return reservation;
        }

        if (reservation.getStatus() != PENDING) {
            throw new ConflictException("Only pending reservations can be confirmed");
        }

        InventorySnapshot inventorySnapshot = findInventorySnapshot(reservation);

        inventorySnapshot.setReservedCount(inventorySnapshot.getReservedCount() - reservation.getQuantity());
        inventorySnapshot.setConfirmedCount(inventorySnapshot.getConfirmedCount() + reservation.getQuantity());
        inventorySnapshotRepository.save(inventorySnapshot);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setOrderId(request.orderId());
        reservation.setConfirmedAt(OffsetDateTime.now());

        return reservationRepository.save(reservation);
    }

    private InventorySnapshot findInventorySnapshot(Reservation reservation) {
        return inventorySnapshotRepository.findByEventOccurrenceIdAndTicketTypeId(reservation.getEventOccurrenceId(), reservation.getTicketTypeId())
                .orElseThrow(() -> new NotFoundException("Inventory snapshot not found"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation cancelReservationInNewTx(UUID id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found!"));

        if (reservation.getStatus() != PENDING) {
            return reservation;
        }

        InventorySnapshot inventorySnapshot = findInventorySnapshot(reservation);
        if (inventorySnapshot.getReservedCount() < reservation.getQuantity()) {
            throw new ConflictException("Snapshot reserved count is lower than reservation quantity");
        }

        inventorySnapshot.setReservedCount(inventorySnapshot.getReservedCount() - reservation.getQuantity());
        inventorySnapshotRepository.save(inventorySnapshot);

        reservation.setStatus(ReservationStatus.CANCELED);
        return reservationRepository.save(reservation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void expireReservationInNewTx(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || reservation.getStatus() != PENDING) {
            return;
        }

        InventorySnapshot snapshot = inventorySnapshotRepository
                .findByEventOccurrenceIdAndTicketTypeId(
                        reservation.getEventOccurrenceId(),
                        reservation.getTicketTypeId()
                )
                .orElse(null);

        if (snapshot != null) {
            if (snapshot.getReservedCount() < reservation.getQuantity()) {
                throw new ConflictException("Snapshot reserved count is lower than reservation quantity");
            }
            snapshot.setReservedCount(snapshot.getReservedCount() - reservation.getQuantity());
            inventorySnapshotRepository.save(snapshot);
        }

        reservation.setStatus(EXPIRED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void expirePendingReservations() {
        List<Reservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                PENDING,
                OffsetDateTime.now()
        );

        for (Reservation reservation : expired) {
            if (reservation.getStatus() != PENDING) {
                continue;
            }

            InventorySnapshot snapshot = inventorySnapshotRepository
                    .getAvailability(
                            reservation.getEventOccurrenceId(),
                            reservation.getTicketTypeId()
                    )
                    .orElse(null);

            if (snapshot != null) {
                snapshot.setReservedCount(snapshot.getReservedCount() - reservation.getQuantity());
                inventorySnapshotRepository.saveAndFlush(snapshot);
            }

            reservation.setStatus(EXPIRED);
            reservationRepository.saveAndFlush(reservation);
        }
    }

    public record CreationResult(Reservation reservation, boolean created) {

    }
}
