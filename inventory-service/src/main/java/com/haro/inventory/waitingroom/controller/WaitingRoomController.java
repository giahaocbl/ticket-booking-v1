package com.haro.inventory.waitingroom.controller;

import com.haro.inventory.waitingroom.dto.EnqueueRequest;
import com.haro.inventory.waitingroom.dto.EnqueueResponse;
import com.haro.inventory.waitingroom.dto.HeartbeatRequest;
import com.haro.inventory.waitingroom.dto.HeartbeatResponse;
import com.haro.inventory.waitingroom.dto.LeaveRequest;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigRequest;
import com.haro.inventory.waitingroom.dto.WaitingRoomConfigResponse;
import com.haro.inventory.waitingroom.dto.WaitingRoomStatusResponse;
import com.haro.inventory.waitingroom.service.WaitingRoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/waiting-room/{resourceId}")
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;

    @PostMapping("/enqueue")
    public ResponseEntity<EnqueueResponse> enqueue(
            @PathVariable @NotBlank String resourceId,
            @Valid @RequestBody EnqueueRequest request
    ) {
        EnqueueResponse response = waitingRoomService.enqueue(resourceId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/status")
    public ResponseEntity<WaitingRoomStatusResponse> status(
            @PathVariable @NotBlank String resourceId,
            @RequestParam @NotBlank String token
    ) {
        return ResponseEntity.ok(waitingRoomService.getStatus(resourceId, token));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<HeartbeatResponse> heartbeat(
            @PathVariable @NotBlank String resourceId,
            @Valid @RequestBody HeartbeatRequest request
    ) {
        return ResponseEntity.ok(waitingRoomService.heartbeat(resourceId, request.token()));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<Void> leave(
            @PathVariable @NotBlank String resourceId,
            @Valid @RequestBody LeaveRequest request
    ) {
        waitingRoomService.leave(resourceId, request.token());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/config")
    public ResponseEntity<WaitingRoomConfigResponse> setConfig(
            @PathVariable @NotBlank String resourceId,
            @Valid @RequestBody WaitingRoomConfigRequest request
    ) {
        return ResponseEntity.ok(waitingRoomService.setConfig(resourceId, request));
    }

    @GetMapping("/config")
    public ResponseEntity<WaitingRoomConfigResponse> getConfig(
            @PathVariable @NotBlank String resourceId
    ) {
        return ResponseEntity.ok(waitingRoomService.getConfig(resourceId));
    }
}
