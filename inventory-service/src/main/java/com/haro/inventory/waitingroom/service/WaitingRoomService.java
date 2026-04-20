package com.haro.inventory.waitingroom.service;

import com.haro.common.web.NotFoundException;
import com.haro.inventory.waitingroom.config.WaitingRoomProperties;
import com.haro.inventory.waitingroom.dto.EnqueueResponse;
import com.haro.inventory.waitingroom.dto.HeartbeatResponse;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigRequest;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigResponse;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatus;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomService {

    private static final String CFG_CAPACITY = "capacity";
    private static final String CFG_PASS_TTL_MS = "passTtlMs";
    private static final String CFG_ENABLED = "enabled";

    private final StringRedisTemplate redis;
    private final WaitingRoomProperties properties;
    @SuppressWarnings("rawtypes")
    private final RedisScript<List> enqueueScript;
    private final RedisScript<Long> admitScript;
    private final RedisScript<Long> heartbeatScript;

    public EnqueueResponse enqueue(String resourceId, UUID userId) {
        if (!isEffectivelyEnabled(resourceId)) {
            return new EnqueueResponse("bypass", 0L, WaitingRoomStatus.ADMITTED, false);
        }

        String newToken = UUID.randomUUID().toString();
        long nowMs = System.currentTimeMillis();
        long tokenTtlMs = properties.getWaitingTokenTtl().toMillis();

        List<String> keys = List.of(
                WaitingRoomKeys.seq(resourceId),
                WaitingRoomKeys.queue(resourceId),
                WaitingRoomKeys.userIndex(resourceId),
                WaitingRoomKeys.ROOMS_REGISTRY
        );
        Object[] args = new Object[]{
                WaitingRoomKeys.tokenHashPrefix(resourceId),
                resourceId,
                newToken,
                userId.toString(),
                Long.toString(nowMs),
                Long.toString(tokenTtlMs)
        };

        @SuppressWarnings("rawtypes")
        List result = redis.execute(enqueueScript, keys, args);
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Waiting room enqueue returned unexpected payload: " + result);
        }
        String token = (String) result.get(0);
        long position = ((Number) result.get(1)).longValue();
        String flag = (String) result.get(2);

        WaitingRoomStatus status = position == 0 ? WaitingRoomStatus.ADMITTED : WaitingRoomStatus.WAITING;
        boolean existing = "EXISTING".equals(flag);
        if (log.isDebugEnabled()) {
            log.debug("Waiting-room enqueue resourceId={} user={} token={} position={} flag={}",
                    resourceId, userId, token, position, flag);
        }
        return new EnqueueResponse(token, position, status, existing);
    }

    public WaitingRoomStatusResponse getStatus(String resourceId, String token) {
        String tokenKey = WaitingRoomKeys.tokenHash(resourceId, token);
        Map<Object, Object> fields = redis.opsForHash().entries(tokenKey);
        if (fields == null || fields.isEmpty()) {
            return new WaitingRoomStatusResponse(token, WaitingRoomStatus.NOT_FOUND, null, null);
        }

        String status = asString(fields.get("status"));
        if ("ADMITTED".equals(status)) {
            Long expiresAt = asLong(fields.get("expiresAt"));
            return new WaitingRoomStatusResponse(token, WaitingRoomStatus.ADMITTED, null, expiresAt);
        }

        Long rank = redis.opsForZSet().rank(WaitingRoomKeys.queue(resourceId), token);
        if (rank == null) {
            return new WaitingRoomStatusResponse(token, WaitingRoomStatus.NOT_FOUND, null, null);
        }
        return new WaitingRoomStatusResponse(token, WaitingRoomStatus.WAITING, rank + 1, null);
    }

    public HeartbeatResponse heartbeat(String resourceId, String token) {
        long nowMs = System.currentTimeMillis();
        long passTtlMs = resolvePassTtlMs(resourceId);

        List<String> keys = List.of(
                WaitingRoomKeys.active(resourceId),
                WaitingRoomKeys.tokenHash(resourceId, token)
        );
        Long result = redis.execute(heartbeatScript, keys,
                token, Long.toString(nowMs), Long.toString(passTtlMs));

        if (result == null || result == 0L) {
            throw new NotFoundException("Token is not admitted or has already expired");
        }
        return new HeartbeatResponse(result);
    }

    public void leave(String resourceId, String token) {
        String tokenKey = WaitingRoomKeys.tokenHash(resourceId, token);
        Object userIdRaw = redis.opsForHash().get(tokenKey, "userId");

        redis.opsForZSet().remove(WaitingRoomKeys.queue(resourceId), token);
        redis.opsForZSet().remove(WaitingRoomKeys.active(resourceId), token);
        redis.delete(tokenKey);

        if (userIdRaw != null) {
            String currentMapped = (String) redis.opsForHash().get(
                    WaitingRoomKeys.userIndex(resourceId), userIdRaw.toString());
            if (token.equals(currentMapped)) {
                redis.opsForHash().delete(WaitingRoomKeys.userIndex(resourceId), userIdRaw.toString());
            }
        }
    }

    public WaitingRoomConfigResponse setConfig(String resourceId, WaitingRoomConfigRequest request) {
        long passTtlMs = request.admissionPassTtlSeconds() != null
                ? request.admissionPassTtlSeconds() * 1000L
                : properties.getAdmissionPassTtl().toMillis();

        String configKey = WaitingRoomKeys.config(resourceId);
        redis.opsForHash().put(configKey, CFG_CAPACITY, Integer.toString(request.capacity()));
        redis.opsForHash().put(configKey, CFG_PASS_TTL_MS, Long.toString(passTtlMs));
        if (request.enabled() != null) {
            redis.opsForHash().put(configKey, CFG_ENABLED, Boolean.toString(request.enabled()));
        }
        redis.opsForSet().add(WaitingRoomKeys.ROOMS_REGISTRY, resourceId);

        return getConfig(resourceId);
    }

    public WaitingRoomConfigResponse getConfig(String resourceId) {
        int capacity = resolveCapacity(resourceId);
        long passTtlMs = resolvePassTtlMs(resourceId);
        long queueDepth = nullSafe(redis.opsForZSet().zCard(WaitingRoomKeys.queue(resourceId)));
        long activeCount = nullSafe(redis.opsForZSet().zCard(WaitingRoomKeys.active(resourceId)));
        boolean enabled = isEffectivelyEnabled(resourceId);

        return new WaitingRoomConfigResponse(
                capacity,
                TimeUnit.MILLISECONDS.toSeconds(passTtlMs),
                properties.getWaitingTokenTtl().toSeconds(),
                queueDepth,
                activeCount,
                enabled
        );
    }

    /**
     * Promote up to {@code capacity - active} tokens from queue into the active set.
     * Atomic via Lua. Safe to invoke from multiple nodes; Redis serializes scripts.
     * @return admitted count on this tick.
     */
    public long promote(String resourceId) {
        int capacity = resolveCapacity(resourceId);
        long passTtlMs = resolvePassTtlMs(resourceId);
        long nowMs = System.currentTimeMillis();

        List<String> keys = List.of(
                WaitingRoomKeys.active(resourceId),
                WaitingRoomKeys.queue(resourceId)
        );
        Long admitted = redis.execute(admitScript, keys,
                WaitingRoomKeys.tokenHashPrefix(resourceId),
                Integer.toString(capacity),
                Long.toString(nowMs),
                Long.toString(passTtlMs));
        return admitted == null ? 0L : admitted;
    }

    public Set<String> listRooms() {
        Set<String> rooms = redis.opsForSet().members(WaitingRoomKeys.ROOMS_REGISTRY);
        return rooms == null ? Set.of() : rooms;
    }

    public void unregisterRoomIfEmpty(String resourceId) {
        long queueDepth = nullSafe(redis.opsForZSet().zCard(WaitingRoomKeys.queue(resourceId)));
        long activeCount = nullSafe(redis.opsForZSet().zCard(WaitingRoomKeys.active(resourceId)));
        if (queueDepth == 0 && activeCount == 0) {
            redis.opsForSet().remove(WaitingRoomKeys.ROOMS_REGISTRY, resourceId);
        }
    }

    private int resolveCapacity(String resourceId) {
        Object raw = redis.opsForHash().get(WaitingRoomKeys.config(resourceId), CFG_CAPACITY);
        if (raw == null) {
            return properties.getDefaultCapacity();
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            log.warn("Invalid capacity config for resource={}, falling back to default", resourceId);
            return properties.getDefaultCapacity();
        }
    }

    /**
     * Effective enable flag: {@code globalEnabled && roomEnabled}. Per-room flag defaults to
     * {@code true} when not explicitly configured, so existing rooms keep behaving as before.
     */
    private boolean isEffectivelyEnabled(String resourceId) {
        if (!properties.isEnabled()) {
            return false;
        }
        Object raw = redis.opsForHash().get(WaitingRoomKeys.config(resourceId), CFG_ENABLED);
        if (raw == null) {
            return true;
        }
        return Boolean.parseBoolean(raw.toString());
    }

    private long resolvePassTtlMs(String resourceId) {
        Object raw = redis.opsForHash().get(WaitingRoomKeys.config(resourceId), CFG_PASS_TTL_MS);
        if (raw == null) {
            return properties.getAdmissionPassTtl().toMillis();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            log.warn("Invalid pass TTL config for resource={}, falling back to default", resourceId);
            return properties.getAdmissionPassTtl().toMillis();
        }
    }

    private static String asString(Object raw) {
        return raw == null ? null : raw.toString();
    }

    private static Long asLong(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
