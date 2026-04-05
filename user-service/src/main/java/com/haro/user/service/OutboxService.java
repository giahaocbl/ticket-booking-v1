package com.haro.user.service;

import com.haro.user.entity.OutboxEvent;
import com.haro.user.event.EmailNotificationEvent;
import com.haro.user.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String TOPIC = "notification.email";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(EmailNotificationEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("USER")
                .aggregateId(event.userId())
                .eventType(event.type())
                .topic(TOPIC)
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
