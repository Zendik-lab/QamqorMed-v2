package com.qamqormed.dto;

import com.qamqormed.entity.TimeSlot;

import java.time.LocalDateTime;

public record TimeSlotResponse(Long id, LocalDateTime startsAt, LocalDateTime endsAt) {
    public static TimeSlotResponse from(TimeSlot slot) {
        return new TimeSlotResponse(slot.getId(), slot.getStartsAt(), slot.getEndsAt());
    }
}
