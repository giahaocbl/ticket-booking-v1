package com.haro.user.dto;

import java.util.UUID;

public record AuthResponse(String accessToken,
                           String refreshToken,
                           UUID sessionId,
                           UserResponse userResponse) {
}
