package com.haro.inventory.repository;

import com.haro.inventory.entity.Reservation;
import com.haro.inventory.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, OffsetDateTime expiresAt);
}
