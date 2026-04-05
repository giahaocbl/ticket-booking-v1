package com.haro.user.dto;

import java.util.UUID;

public record PasswordResetResponse(UUID tokenId, String tokenRaw) {
}
