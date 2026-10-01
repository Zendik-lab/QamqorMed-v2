package com.qamqormed.dto;

import com.qamqormed.entity.ScheduleExceptionType;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionRequest(LocalDate date, ScheduleExceptionType type, LocalTime startTime, LocalTime endTime, String note) {}
