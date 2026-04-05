package com.haro.payment.consumer;

import com.haro.payment.dto.CreatePaymentRequest;
import com.haro.payment.dto.PaymentResponse;
import com.haro.payment.entity.PaymentStatus;
import com.haro.payment.event.OrderCreatedEvent;
import com.haro.payment.event.PaymentFailedEvent;
import com.haro.payment.event.PaymentSucceededEvent;
import com.haro.payment.event.SagaTopics;
import com.haro.payment.service.OutboxService;
import com.haro.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
public class SagaOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaOrderConsumer.class);
    private static final String DEFAULT_PROVIDER = "MOCK";

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final OutboxService outboxService;

    public SagaOrderConsumer(ObjectMapper objectMapper,
                             PaymentService paymentService,
                             OutboxService outboxService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.outboxService = outboxService;
    }

    @KafkaListener(topics = SagaTopics.ORDER, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onOrderCreated(String message) {
        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(message, OrderCreatedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize OrderCreated event, skipping: {}", message, ex);
            return;
        }

        log.info("Received OrderCreated orderId={} amount={}", event.orderId(), event.amount());

        String idempotencyKey = "saga-pay-" + event.orderId();

        CreatePaymentRequest payRequest = new CreatePaymentRequest(
                event.orderId(),
                event.userId(),
                event.amount(),
                event.currency(),
                DEFAULT_PROVIDER,
                idempotencyKey
        );

        PaymentResponse result = paymentService.createPayment(payRequest);

        if (result.status() == PaymentStatus.PAID) {
            outboxService.saveEvent(
                    SagaTopics.PAYMENT,
                    "PAYMENT",
                    result.id(),
                    "PaymentSucceeded",
                    new PaymentSucceededEvent(
                            event.orderId(),
                            result.id(),
                            event.reservationId(),
                            result.amount()
                    )
            );
            log.info("Payment succeeded paymentId={} orderId={}", result.id(), event.orderId());
        } else {
            String reason = result.failureMessage() != null
                    ? result.failureMessage()
                    : "Payment failed with status " + result.status();

            outboxService.saveEvent(
                    SagaTopics.PAYMENT,
                    "PAYMENT",
                    result.id(),
                    "PaymentFailed",
                    new PaymentFailedEvent(
                            event.orderId(),
                            event.reservationId(),
                            reason
                    )
            );
            log.warn("Payment failed paymentId={} orderId={} reason={}", result.id(), event.orderId(), reason);
        }
    }
}
