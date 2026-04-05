package com.haro.user.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit.auth")
public record AuthRateLimitProperties(
        long windowSeconds,
        long loginLimit,
        long refreshLimit,
        long logoutLimit,
        long verifyEmailLimit,
        long resendVerificationLimit,
        long passwordResetLimit,
        long passwordResetConfirmLimit
) {
}
