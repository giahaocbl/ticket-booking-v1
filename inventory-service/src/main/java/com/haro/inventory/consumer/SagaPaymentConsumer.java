package com.haro.inventory.consumer;

import com.haro.inventory.dto.ConfirmReservationRequest;
import com.haro.inventory.service.ReservationService;
import com.haro.inventory.event.PaymentResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SagaPaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaPaymentConsumer.class);
    private static final String TOPIC = "saga.payment";

    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;

    public SagaPaymentConsumer(ObjectMapper objectMapper, ReservationService reservationService) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
    }

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentResult(String message) {
        PaymentResultEvent event;
        try {
            event = objectMapper.readValue(message, PaymentResultEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize payment event, skipping: {}", message, ex);
            return;
        }

        if (event.reservationId() == null) {
            log.warn("Payment event has no reservationId, skipping orderId={}", event.orderId());
            return;
        }

        switch (event.eventType()) {
            case "PaymentSucceeded" -> {
                log.info("Payment succeeded, confirming reservationId={} for orderId={}",
                        event.reservationId(), event.orderId());
                try {
                    reservationService.confirmReservation(
                            event.reservationId(),
                            new ConfirmReservationRequest(event.orderId())
                    );
                } catch (Exception ex) {
                    log.error("Failed to confirm reservationId={}", event.reservationId(), ex);
                    throw ex;
                }
            }
            case "PaymentFailed" -> {
                log.info("Payment failed, cancelling reservationId={} for orderId={}",
                        event.reservationId(), event.orderId());
                try {
                    reservationService.cancelReservation(event.reservationId());
                } catch (Exception ex) {
                    log.error("Failed to cancel reservationId={}", event.reservationId(), ex);
                    throw ex;
                }
            }
            default -> log.warn("Unknown payment event type: {}", event.eventType());
        }
    }
}
