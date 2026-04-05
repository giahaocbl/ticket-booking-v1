package com.haro.inventory.redis;

import com.haro.inventory.service.ReservationService;
import com.haro.inventory.service.ReservationTtlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReservationExpirationListener extends KeyExpirationEventMessageListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationListener.class);

    private final ReservationTtlService reservationTtlService;
    private final ReservationService reservationService;

    public ReservationExpirationListener(
            RedisMessageListenerContainer listenerContainer,
            ReservationTtlService reservationTtlService,
            ReservationService reservationService
    ) {
        super(listenerContainer);
        setKeyspaceNotificationsConfigParameter("Ex");
        this.reservationTtlService = reservationTtlService;
        this.reservationService = reservationService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (!reservationTtlService.isReservationTtlKey(expiredKey)) {
            return;
        }

        try {
            UUID reservationId = reservationTtlService.reservationIdFromKey(expiredKey);
            reservationService.expireReservation(reservationId);
            log.debug("Expired reservation from redis ttl key, reservationId={}", reservationId);
        } catch (Exception ex) {
            log.warn("Failed handling redis expiration event for key={}", expiredKey, ex);
        }
    }
}
