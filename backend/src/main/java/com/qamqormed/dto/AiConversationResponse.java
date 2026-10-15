package com.qamqormed.dto;

import com.qamqormed.entity.AiConversation;

import java.time.Instant;

public record AiConversationResponse(Long id, String title, String role, Instant createdAt, Instant updatedAt) {
    public static AiConversationResponse from(AiConversation conversation) {
        return new AiConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getOwnerRole().name(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
