package com.haro.user.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<List> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
                local key = KEYS[1]
                local limit = tonumber(ARGV[1])
                local windowSeconds = tonumber(ARGV[2])

                local current = redis.call('incr', key)
                if current == 1 then
                    redis.call('expire', key, windowSeconds)
                end

                local ttl = redis.call('ttl', key)
                if ttl < 0 then ttl = windowSeconds end

                local remaining = limit - current
                if remaining < 0 then remaining = 0 end

                if current > limit then
                    return {0, remaining, ttl}
                end

                return {1, remaining, ttl}
            """,
            List.class
    );

    public RateLimitDecision tryConsume(String key, long limit, Duration window) {
        List<?> result = redisTemplate.execute(
                FIXED_WINDOW_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(window.getSeconds())
        );

        if (result == null || result.size() < 3) {
            // Fail-open to avoid blocking legitimate users due to a Redis glitch.
            return new RateLimitDecision(true, limit, window.getSeconds());
        }

        long allowedFlag = ((Number) result.get(0)).longValue();
        long remaining = ((Number) result.get(1)).longValue();
        long retryAfterSeconds = ((Number) result.get(2)).longValue();

        return new RateLimitDecision(allowedFlag == 1, remaining, retryAfterSeconds);
    }
}