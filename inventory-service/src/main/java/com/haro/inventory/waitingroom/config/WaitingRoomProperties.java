package com.haro.inventory.waitingroom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "waiting-room")
public class WaitingRoomProperties {

    /** Master switch. When false, /enqueue immediately returns ADMITTED. */
    private boolean enabled = true;

    /** Default max concurrently admitted users per room (overridable per room). */
    private int defaultCapacity = 100;

    /** Admission pass lifetime. Clients must heartbeat or finish booking within this window. */
    private Duration admissionPassTtl = Duration.ofMinutes(2);

    /** TTL for waiting-room tokens (protects against forever-stale entries). */
    private Duration waitingTokenTtl = Duration.ofHours(1);
}
