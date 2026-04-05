package com.haro.notification.repository;

import com.haro.notification.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {
    boolean existsByReferenceTypeAndReferenceIdAndStatus(String referenceType, UUID referenceId, String status);
}
