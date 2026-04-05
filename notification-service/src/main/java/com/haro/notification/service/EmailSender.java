package com.haro.notification.service;

public interface EmailSender {
    String sendEmail(String to, String subject, String htmlBody);
}
