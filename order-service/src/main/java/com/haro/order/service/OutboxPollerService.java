package com.haro.order.service;

import com.haro.order.entity.OutboxEvent;
import com.haro.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxPollerService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPollerService(OutboxEventRepository outboxEventRepository,
                               KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox poller found {} pending events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get();

                event.setStatus("PUBLISHED");
                event.setPublishedAt(OffsetDateTime.now());
                outboxEventRepository.save(event);

                log.debug("Published outbox event id={} type={}", event.getId(), event.getEventType());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={} type={}, will retry on next poll",
                        event.getId(), event.getEventType(), ex);
                break;
            }
        }
    }
}
