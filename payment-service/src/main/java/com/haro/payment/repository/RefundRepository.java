package com.haro.payment.repository;

import com.haro.payment.entity.Refund;
import com.haro.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByIdempotencyKey(String idempotencyKey);

    List<Refund> findByPaymentId(UUID paymentId);

    List<Refund> findByPaymentIdAndStatus(UUID paymentId, RefundStatus status);
}
