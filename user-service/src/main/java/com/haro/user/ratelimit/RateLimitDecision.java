package com.haro.user.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long remaining,
        long retryAfterSeconds
) {
}

