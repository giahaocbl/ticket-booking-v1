package com.haro.inventory.service;

import com.haro.common.web.ConflictException;
import com.haro.inventory.entity.InventorySnapshot;
import com.haro.inventory.entity.Reservation;
import com.haro.inventory.entity.ReservationStatus;
import com.haro.inventory.dto.ConfirmReservationRequest;
import com.haro.inventory.dto.CreateReservationRequest;
import com.haro.inventory.dto.ReservationDto;
import com.haro.inventory.repository.InventorySnapshotRepository;
import com.haro.inventory.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static com.haro.inventory.entity.ReservationStatus.PENDING;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReservationTtlService reservationTtlService;
    private final ReservationTxService reservationTxService;
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final int MAX_ATTEMPTS = 3;

    private <T> T withOptimisticLockRetries(int maxAttempts, java.util.function.Supplier<T> action) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (ObjectOptimisticLockingFailureException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }
                try {
                    long base = 25L * attempt;
                    long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(25L);
                    Thread.sleep(base + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    public ReservationDto createReservation(CreateReservationRequest request) {
        return reservationRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::mapToDto)
                .orElseGet(() -> performReserve(request));
    }

    private ReservationDto performReserve(CreateReservationRequest request) {
        try {
            ReservationTxService.CreationResult result = withOptimisticLockRetries(MAX_ATTEMPTS, () -> reservationTxService.createReservationInNewTx(request));

            if (result.created() && result.reservation().getStatus() == PENDING) {
                reservationTtlService.scheduleExpiration(result.reservation().getId(), request.ttlSeconds());
            }

            return mapToDto(result.reservation());
        } catch (ObjectOptimisticLockingFailureException exception) {
            log.debug("Optimistic lock conflict while reserving inventory after retries");
        } catch (DataIntegrityViolationException exception) {
            // Most likely idempotency conflict: another request created it concurrently.
            Reservation concurrent = reservationRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new ConflictException("Reservation already exists for this idempotency key"));
            return mapToDto(concurrent);
        }

        throw new ConflictException("Reservation could not be created. Please retry.");
    }

    private ReservationDto mapToDto(Reservation reservation) {
        return new ReservationDto(reservation.getId(), reservation.getEventOccurrenceId(), reservation.getTicketTypeId(),
                reservation.getUserId(), reservation.getQuantity(),
                reservation.getStatus(), reservation.getExpiresAt(),
                reservation.getCreatedAt(), reservation.getUpdatedAt());
    }

    public ReservationDto confirmReservation(UUID id, ConfirmReservationRequest request) {
        try {
            Reservation reservation = withOptimisticLockRetries(
                    4,
                    () -> reservationTxService.confirmReservationInNewTx(id, request)
            );
            reservationTtlService.clearExpiration(id);
            return mapToDto(reservation);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.debug("Optimistic lock conflict while confirming reservation after retries");
        }

        throw new ConflictException("Could not confirm reservation. Please retry.");
    }

    public ReservationDto cancelReservation(UUID id) {
        try {
            Reservation reservation = withOptimisticLockRetries(
                    4,
                    () -> reservationTxService.cancelReservationInNewTx(id)
            );
            reservationTtlService.clearExpiration(id);
            return mapToDto(reservation);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.debug("Optimistic lock conflict while confirming reservation after retries");
        }

        throw new ConflictException("Could not confirm reservation. Please retry.");
    }

    private InventorySnapshot findInventorySnapshot(Reservation reservation) {
        return inventorySnapshotRepository.getAvailability(reservation.getEventOccurrenceId(), reservation.getTicketTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Inventory snapshot not found"));
    }


    @Transactional(readOnly = true)
    public Page<ReservationDto> getReservationsForUser(UUID userId, ReservationStatus status, Pageable pageable) {
        Page<Reservation> reservations;
        if (status != null) {
            reservations = reservationRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            reservations = reservationRepository.findByUserId(userId, pageable);
        }
        return reservations.map(this::mapToDto);
    }

    public void expireReservation(UUID reservationId) {
        try {
            withOptimisticLockRetries(4, () -> {
                reservationTxService.expireReservationInNewTx(reservationId);
                return null;
            });
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.debug("Optimistic lock conflict while expiring reservationId={} after retries", reservationId);
        } catch (Exception ex) {
            log.warn("Failed to expire reservationId={}", reservationId, ex);
            throw ex;
        }
    }
}
