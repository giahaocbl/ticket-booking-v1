package com.haro.gateway.security;


import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UserHeadersGatewayFilter implements GlobalFilter, Ordered {

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_USER_EMAIL = "X-User-Email";
    public static final String HDR_USER_ROLES = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                // Prevent header spoofing from clients
                .headers(h -> {
                    h.remove(HDR_USER_ID);
                    h.remove(HDR_USER_EMAIL);
                    h.remove(HDR_USER_ROLES);
                })
                .build();

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .defaultIfEmpty(null)
                .flatMap(auth -> {
                    if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !jwtAuth.isAuthenticated()) {
                        return chain.filter(exchange.mutate().request(sanitized).build());
                    }

                    String userId = jwtAuth.getToken().getSubject();
                    String email = jwtAuth.getToken().getClaimAsString("email");
                    List<String> roles = jwtAuth.getToken().getClaimAsStringList("roles");
                    String rolesHeader = roles != null ? String.join(",", roles) : "";

                    ServerHttpRequest enriched = sanitized.mutate()
                            .header(HDR_USER_ID, userId)
                            .header(HDR_USER_EMAIL, email != null ? email : "")
                            .header(HDR_USER_ROLES, rolesHeader)
                            .build();

                    return chain.filter(exchange.mutate().request(enriched).build());
                });
    }

    @Override
    public int getOrder() {
        // Run after authentication so principal is available
        return 10;
    }
}

