package com.haro.inventory.waitingroom.controller;

import com.haro.inventory.waitingroom.dto.EnqueueRequest;
import com.haro.inventory.waitingroom.dto.EnqueueResponse;
import com.haro.inventory.waitingroom.dto.HeartbeatRequest;
import com.haro.inventory.waitingroom.dto.HeartbeatResponse;
import com.haro.inventory.waitingroom.dto.LeaveRequest;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigRequest;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigResponse;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatus;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatusResponse;
import com.haro.inventory.waitingroom.service.WaitingRoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingRoomControllerTest {

    @Mock
    private WaitingRoomService waitingRoomService;

    @InjectMocks
    private WaitingRoomController controller;

    private static final String RESOURCE = "event-123";

    @Test
    void enqueueReturnsCreatedAndDelegates() {
        UUID userId = UUID.randomUUID();
        EnqueueResponse expected = new EnqueueResponse("tok-1", 5L, WaitingRoomStatus.WAITING, false);
        when(waitingRoomService.enqueue(RESOURCE, userId)).thenReturn(expected);

        ResponseEntity<EnqueueResponse> response = controller.enqueue(RESOURCE, new EnqueueRequest(userId));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(waitingRoomService).enqueue(RESOURCE, userId);
    }

    @Test
    void statusReturnsOk() {
        WaitingRoomStatusResponse expected = new WaitingRoomStatusResponse(
                "tok-1", WaitingRoomStatus.WAITING, 3L, null);
        when(waitingRoomService.getStatus(RESOURCE, "tok-1")).thenReturn(expected);

        ResponseEntity<WaitingRoomStatusResponse> response = controller.status(RESOURCE, "tok-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void heartbeatReturnsNewExpiration() {
        HeartbeatResponse expected = new HeartbeatResponse(12345L);
        when(waitingRoomService.heartbeat(RESOURCE, "tok-1")).thenReturn(expected);

        ResponseEntity<HeartbeatResponse> response = controller.heartbeat(RESOURCE, new HeartbeatRequest("tok-1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void leaveReturnsNoContent() {
        ResponseEntity<Void> response = controller.leave(RESOURCE, new LeaveRequest("tok-1"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(waitingRoomService).leave(RESOURCE, "tok-1");
    }

    @Test
    void setConfigReturnsCurrentConfig() {
        WaitingRoomConfigRequest request = new WaitingRoomConfigRequest(200, 120L, true);
        WaitingRoomConfigResponse expected = new WaitingRoomConfigResponse(200, 120, 3600, 0, 0, true);
        when(waitingRoomService.setConfig(RESOURCE, request)).thenReturn(expected);

        ResponseEntity<WaitingRoomConfigResponse> response = controller.setConfig(RESOURCE, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void setConfigSupportsDisablingTheRoom() {
        WaitingRoomConfigRequest request = new WaitingRoomConfigRequest(200, 120L, false);
        WaitingRoomConfigResponse expected = new WaitingRoomConfigResponse(200, 120, 3600, 0, 0, false);
        when(waitingRoomService.setConfig(RESOURCE, request)).thenReturn(expected);

        ResponseEntity<WaitingRoomConfigResponse> response = controller.setConfig(RESOURCE, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getConfigReturnsConfig() {
        WaitingRoomConfigResponse expected = new WaitingRoomConfigResponse(100, 120, 3600, 42, 3, true);
        when(waitingRoomService.getConfig(RESOURCE)).thenReturn(expected);

        ResponseEntity<WaitingRoomConfigResponse> response = controller.getConfig(RESOURCE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }
}
