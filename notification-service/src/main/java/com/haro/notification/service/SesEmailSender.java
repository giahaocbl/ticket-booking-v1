package com.haro.notification.service;

import com.haro.notification.config.SesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Service
@Profile("!dev")
public class SesEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesV2Client sesClient;
    private final String fromAddress;

    public SesEmailSender(SesConfig config) {
        this.sesClient = SesV2Client.builder()
                .region(Region.of(config.region()))
                .build();
        this.fromAddress = config.fromAddress();
    }

    @Override
    public String sendEmail(String to, String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        SendEmailResponse response = sesClient.sendEmail(request);
        String messageId = response.messageId();
        log.info("SES email sent to={} messageId={}", to, messageId);
        return messageId;
    }
}

