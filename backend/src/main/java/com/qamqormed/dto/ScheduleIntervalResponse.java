package com.qamqormed.dto;

import com.qamqormed.entity.DoctorScheduleInterval;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleIntervalResponse(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, boolean breakInterval) {
    public static ScheduleIntervalResponse from(DoctorScheduleInterval value) {
        return new ScheduleIntervalResponse(value.getDayOfWeek(), value.getStartTime(), value.getEndTime(), value.isBreakInterval());
    }
}
