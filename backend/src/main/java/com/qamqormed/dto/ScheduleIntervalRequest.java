package com.qamqormed.dto;

import com.qamqormed.entity.ScheduleExceptionType;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleIntervalRequest(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, boolean breakInterval) {}
