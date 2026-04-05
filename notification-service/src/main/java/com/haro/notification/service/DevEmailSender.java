package com.haro.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("dev")
public class DevEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(DevEmailSender.class);

    @Override
    public String sendEmail(String to, String subject, String htmlBody) {
        String fakeMessageId = "dev-" + UUID.randomUUID();
        log.info("""
                [DEV] Email would be sent:
                  To: {}
                  Subject: {}
                  MessageId: {}
                  Body length: {} chars""", to, subject, fakeMessageId, htmlBody.length());
        return fakeMessageId;
    }
}