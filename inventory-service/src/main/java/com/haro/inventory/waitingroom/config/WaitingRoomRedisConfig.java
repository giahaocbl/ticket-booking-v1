package com.haro.inventory.waitingroom.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
@EnableConfigurationProperties(WaitingRoomProperties.class)
public class WaitingRoomRedisConfig {

    @Bean
    @SuppressWarnings({"rawtypes"})
    public DefaultRedisScript<List> waitingRoomEnqueueScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/waiting-room-enqueue.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> waitingRoomAdmitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/waiting-room-admit.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<Long> waitingRoomHeartbeatScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/waiting-room-heartbeat.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
