package com.haro.order.consumer;

import com.haro.order.event.PaymentResultEvent;
import com.haro.order.event.SagaTopics;
import com.haro.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SagaPaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaPaymentConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public SagaPaymentConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = SagaTopics.PAYMENT, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentResult(String message) {
        PaymentResultEvent event;
        try {
            event = objectMapper.readValue(message, PaymentResultEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize payment event, skipping: {}", message, ex);
            return;
        }

        switch (event.eventType()) {
            case "PaymentSucceeded" -> {
                log.info("Payment succeeded for orderId={}, marking order PAID", event.orderId());
                orderService.markOrderPaid(event.orderId());
            }
            case "PaymentFailed" -> {
                String reason = event.reason() != null ? event.reason() : "Payment failed";
                log.info("Payment failed for orderId={}, marking order FAILED", event.orderId());
                orderService.failOrder(event.orderId(), reason);
            }
            default -> log.warn("Unknown payment event type: {}", event.eventType());
        }
    }
}
