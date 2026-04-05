package com.haro.notification.service;


import com.haro.notification.entity.DeliveryLog;
import com.haro.notification.repository.DeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryLogService {

    private final DeliveryLogRepository deliveryLogRepository;


    @Transactional(readOnly = true)
    public boolean isAlreadySent(String referenceType, UUID referenceId) {
        return deliveryLogRepository.existsByReferenceTypeAndReferenceIdAndStatus(
                referenceType,
                referenceId,
                "SENT"
        );
    }


    @Transactional
    public void logSuccess(UUID userId, String templateName, String recipient,
                           String referenceType, UUID referenceId, String providerId) {
        DeliveryLog log = DeliveryLog.builder()
                .userId(userId)
                .channel("EMAIL")
                .templateName(templateName)
                .recipient(recipient)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status("SENT")
                .providerId(providerId)
                .sentAt(OffsetDateTime.now())
                .build();
        try {
            deliveryLogRepository.save(log);
        } catch (DataIntegrityViolationException ex) {
            // Deduplication: another consumer instance already inserted SENT for the same reference.
            // We intentionally do not treat this as an email failure.
        }
    }

    @Transactional
    public void logFailure(UUID userId, String templateName, String recipient,
                           String referenceType, UUID referenceId, String errorMessage) {
        DeliveryLog log = DeliveryLog.builder()
                .userId(userId)
                .channel("EMAIL")
                .templateName(templateName)
                .recipient(recipient)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status("FAILED")
                .providerId(errorMessage)
                .build();
        deliveryLogRepository.save(log);
    }
}

