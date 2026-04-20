package com.haro.inventory.waitingroom.service;

import com.haro.inventory.waitingroom.config.WaitingRoomProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Periodically promotes waiting users into the admitted set for every registered room.
 * The Lua admit script is atomic, so multiple nodes running this scheduler is safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingRoomAdmissionScheduler {

    private final WaitingRoomService waitingRoomService;
    private final WaitingRoomProperties properties;

    @Scheduled(fixedDelayString = "${waiting-room.admission-interval-ms:1000}")
    public void admitTick() {
        if (!properties.isEnabled()) {
            return;
        }

        Set<String> rooms = waitingRoomService.listRooms();
        if (rooms.isEmpty()) {
            return;
        }

        for (String resourceId : rooms) {
            try {
                long admitted = waitingRoomService.promote(resourceId);
                if (admitted > 0 && log.isDebugEnabled()) {
                    log.debug("Admitted {} users from waiting room resourceId={}", admitted, resourceId);
                }
                waitingRoomService.unregisterRoomIfEmpty(resourceId);
            } catch (Exception ex) {
                log.warn("Failed to run admission tick for resourceId={}", resourceId, ex);
            }
        }
    }
}
