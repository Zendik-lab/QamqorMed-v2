package com.qamqormed.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
