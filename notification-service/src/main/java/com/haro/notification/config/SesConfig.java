package com.haro.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.ses")
public record SesConfig(
        String region,
        String fromAddress
) {
}
