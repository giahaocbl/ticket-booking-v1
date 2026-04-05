package com.haro.notification.consumer;

import com.haro.notification.event.EmailNotificationEvent;
import com.haro.notification.service.DeliveryLogService;
import com.haro.notification.service.EmailSender;
import com.haro.notification.service.TemplateRendererService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEmailConsumer.class);

    private final TemplateRendererService templateRenderer;
    private final EmailSender emailSender;
    private final DeliveryLogService deliveryLogService;
    private final String verificationBaseUrl;
    private final String passwordResetBaseUrl;
    private final ObjectMapper objectMapper;

    public KafkaEmailConsumer(
            TemplateRendererService templateRenderer,
            EmailSender emailSender,
            DeliveryLogService deliveryLogService,
            @Value("${app.verification-base-url}") String verificationBaseUrl,
            @Value("${app.password-reset-base-url}") String passwordResetBaseUrl, ObjectMapper objectMapper
    ) {
        this.templateRenderer = templateRenderer;
        this.emailSender = emailSender;
        this.deliveryLogService = deliveryLogService;
        this.verificationBaseUrl = verificationBaseUrl;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "notification.email", groupId = "notification-service")
    public void consume(String rawEvent) {
        log.info(rawEvent);
        EmailNotificationEvent event = objectMapper.readValue(rawEvent, EmailNotificationEvent.class);
        log.info("Received {} event for user={}", event.type(), event.email());

        try {
            switch (event.type()) {
                case "EMAIL_VERIFICATION" -> handleEmailVerification(event);
                case "PASSWORD_RESET" -> handlePasswordReset(event);
                default -> log.warn("Unknown event type: {}", event.type());
            }
        } catch (Exception ex) {
            log.error("Failed to process {} event for user={}", event.type(), event.email(), ex);
        }
    }

    private void handleEmailVerification(EmailNotificationEvent event) {
        String tokenId = event.payload().get("tokenId");
        String rawToken = event.payload().get("rawToken");
        String verificationUrl = verificationBaseUrl + "?tokenId=" + tokenId + "&token=" + rawToken;

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.name() != null ? event.name() : "there");
        variables.put("verificationUrl", verificationUrl);

        String html = templateRenderer.render("email-verification", variables);
        sendAndLog(event, "email-verification", "Verify your email - Ticket Booking",
                html, "EMAIL_VERIFICATION", tokenId != null ? UUID.fromString(tokenId) : null);
    }

    private void handlePasswordReset(EmailNotificationEvent event) {
        String tokenId = event.payload().get("tokenId");
        String rawToken = event.payload().get("rawToken");
        String resetUrl = passwordResetBaseUrl + "?tokenId=" + tokenId + "&token=" + rawToken;

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.name() != null ? event.name() : "there");
        variables.put("resetUrl", resetUrl);

        String html = templateRenderer.render("password-reset", variables);
        sendAndLog(event, "password-reset", "Reset your password - Ticket Booking",
                html, "PASSWORD_RESET", tokenId != null ? UUID.fromString(tokenId) : null);
    }

    private void sendAndLog(EmailNotificationEvent event, String templateName, String subject,
                            String html, String referenceType, UUID referenceId) {
        // Idempotency: at-least-once Kafka delivery can cause duplicates.
        // We only deduplicate successful sends ("SENT") per notification reference (type + tokenId).
        if (referenceId != null && deliveryLogService.isAlreadySent(referenceType, referenceId)) {
            log.info("Skipping duplicate {} email for user={} refId={}",
                    referenceType, event.userId(), referenceId);
            return;
        }
        try {
            String messageId = emailSender.sendEmail(event.email(), subject, html);
            deliveryLogService.logSuccess(event.userId(), templateName, event.email(),
                    referenceType, referenceId, messageId);
        } catch (Exception ex) {
            log.error("Failed to send email to={} template={}", event.email(), templateName, ex);
            deliveryLogService.logFailure(event.userId(), templateName, event.email(),
                    referenceType, referenceId, ex.getMessage());
        }
    }
}
