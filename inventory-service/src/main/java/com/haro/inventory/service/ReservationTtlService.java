package com.haro.inventory.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class ReservationTtlService {

    private static final String KEY_PREFIX = "reservation:ttl:";

    private final StringRedisTemplate redisTemplate;

    public ReservationTtlService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void scheduleExpiration(UUID reservationId, int ttlSeconds) {
        String key = toKey(reservationId);
        redisTemplate.opsForValue().set(key, reservationId.toString(), Duration.ofSeconds(ttlSeconds));
    }

    public void clearExpiration(UUID reservationId) {
        redisTemplate.delete(toKey(reservationId));
    }

    public boolean isReservationTtlKey(String key) {
        return key != null && key.startsWith(KEY_PREFIX);
    }

    public UUID reservationIdFromKey(String key) {
        if (!isReservationTtlKey(key)) {
            throw new IllegalArgumentException("Unsupported reservation ttl key: " + key);
        }
        String idPart = key.substring(KEY_PREFIX.length());
        return UUID.fromString(idPart);
    }

    private String toKey(UUID reservationId) {
        return KEY_PREFIX + reservationId;
    }
}
