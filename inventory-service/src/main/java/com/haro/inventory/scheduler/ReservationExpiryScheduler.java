package com.haro.inventory.scheduler;

import com.haro.inventory.service.ReservationTxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReservationTxService reservationTxService;

    // Run every 30 seconds
    @Scheduled(fixedDelay = 30_000)
    public void expireReservations() {
        reservationTxService.expirePendingReservations();
    }
}