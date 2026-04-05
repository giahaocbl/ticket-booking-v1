package com.haro.user.security;

import com.haro.common.web.ApiErrorResponse;
import com.haro.user.ratelimit.AuthRateLimitProperties;
import com.haro.user.ratelimit.RateLimitDecision;
import com.haro.user.ratelimit.RedisRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimiter redisRateLimiter;
    private final AuthRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Let preflight requests pass through.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ruleKey = resolveRuleKey(request);
        if (ruleKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long limit = resolveLimit(ruleKey);
        long windowSeconds = properties.windowSeconds();

        String clientIp = extractClientIp(request);
        String safeIp = clientIp.replace(':', '_');
        String redisKey = "rate-limit:auth:" + ruleKey + ":" + safeIp;

        RateLimitDecision decision = redisRateLimiter.tryConsume(
                redisKey,
                limit,
                java.time.Duration.ofSeconds(windowSeconds)
        );

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiErrorResponse body = new ApiErrorResponse(
                    OffsetDateTime.now(),
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded. Please retry later.",
                    request.getRequestURI()
            );

            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveRuleKey(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Normalize exact paths.
        if (uri == null) return null;

        return switch (uri) {
            case "/api/auth/login" -> "login";
            case "/api/auth/refresh" -> "refresh";
            case "/api/auth/logout" -> "logout";
            case "/api/auth/verify-email" -> "verify-email";
            case "/api/auth/resend-verification" -> "resend-verification";
            case "/api/auth/password-reset" -> "password-reset";
            case "/api/auth/password-reset/confirm" -> "password-reset-confirm";
            default -> null;
        };
    }

    private long resolveLimit(String ruleKey) {
        return switch (ruleKey) {
            case "login" -> properties.loginLimit();
            case "refresh" -> properties.refreshLimit();
            case "logout" -> properties.logoutLimit();
            case "verify-email" -> properties.verifyEmailLimit();
            case "resend-verification" -> properties.resendVerificationLimit();
            case "password-reset" -> properties.passwordResetLimit();
            case "password-reset-confirm" -> properties.passwordResetConfirmLimit();
            default -> properties.loginLimit();
        };
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }
}
