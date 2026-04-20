package com.haro.inventory.waitingroom.service;

import com.haro.inventory.waitingroom.config.WaitingRoomProperties;
import com.haro.inventory.waitingroom.dto.EnqueueResponse;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingRoomServiceEnabledTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private HashOperations<String, Object, Object> hashOps;
    @Mock
    @SuppressWarnings("rawtypes")
    private RedisScript<List> enqueueScript;
    @Mock
    private RedisScript<Long> admitScript;
    @Mock
    private RedisScript<Long> heartbeatScript;

    private WaitingRoomProperties properties;
    private WaitingRoomService service;

    private static final String RESOURCE = "event-abc";
    private static final String CFG_KEY = "wr:{" + RESOURCE + "}:cfg";

    @BeforeEach
    void setUp() {
        properties = new WaitingRoomProperties();
        service = new WaitingRoomService(redis, properties, enqueueScript, admitScript, heartbeatScript);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enqueueBypassesWhenGlobalDisabled() {
        properties.setEnabled(false);

        EnqueueResponse response = service.enqueue(RESOURCE, UUID.randomUUID());

        assertEquals(WaitingRoomStatus.ADMITTED, response.status());
        assertEquals(0L, response.position());
        assertEquals("bypass", response.token());
        verify(redis, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void enqueueBypassesWhenRoomDisabled() {
        properties.setEnabled(true);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(eq(CFG_KEY), eq("enabled"))).thenReturn("false");

        EnqueueResponse response = service.enqueue(RESOURCE, UUID.randomUUID());

        assertEquals(WaitingRoomStatus.ADMITTED, response.status());
        assertEquals("bypass", response.token());
        verify(redis, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void enqueueProceedsWhenRoomEnabledOrUnset() {
        properties.setEnabled(true);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(eq(CFG_KEY), eq("enabled"))).thenReturn(null);
        when(redis.execute(eq(enqueueScript), anyList(), any(Object[].class)))
                .thenReturn(List.of("tok-1", 3L, "NEW"));

        EnqueueResponse response = service.enqueue(RESOURCE, UUID.randomUUID());

        assertEquals(WaitingRoomStatus.WAITING, response.status());
        assertEquals(3L, response.position());
        assertEquals("tok-1", response.token());
        assertFalse(response.existing());
    }
}
