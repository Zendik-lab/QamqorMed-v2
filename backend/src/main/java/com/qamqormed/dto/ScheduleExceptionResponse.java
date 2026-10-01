package com.qamqormed.dto;

import com.qamqormed.entity.ScheduleException;
import com.qamqormed.entity.ScheduleExceptionType;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionResponse(LocalDate date, ScheduleExceptionType type, LocalTime startTime, LocalTime endTime, String note) {
    public static ScheduleExceptionResponse from(ScheduleException value) {
        return new ScheduleExceptionResponse(value.getDate(), value.getType(), value.getStartTime(), value.getEndTime(), value.getNote());
    }
}
