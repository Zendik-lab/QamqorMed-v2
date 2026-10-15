package com.qamqormed.dto;

import com.qamqormed.entity.AiMessage;

import java.time.Instant;

public record AiMessageResponse(Long id, String role, String content, Instant createdAt) {
    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(message.getId(), message.getMessageType().name(), message.getContent(), message.getCreatedAt());
    }
}
