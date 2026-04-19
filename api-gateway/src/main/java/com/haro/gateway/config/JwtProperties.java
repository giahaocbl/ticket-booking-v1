package com.haro.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret
) {
    public JwtProperties {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("jwt.secret must be configured");
        }
    }
}
