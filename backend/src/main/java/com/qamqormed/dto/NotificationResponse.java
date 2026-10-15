package com.qamqormed.dto;

import com.qamqormed.entity.Notification;
import java.time.Instant;

public record NotificationResponse(Long id, String type, String title, String message, Instant createdAt,
                                   Instant readAt, boolean read, Long relatedAppointmentId, String actionUrl) {
    public static NotificationResponse from(Notification value) {
        return new NotificationResponse(value.getId(), value.getType().name(), value.getTitle(), value.getMessage(),
                value.getCreatedAt(), value.getReadAt(), value.isRead(),
                value.getRelatedAppointment() == null ? null : value.getRelatedAppointment().getId(), value.getActionUrl());
    }
}
