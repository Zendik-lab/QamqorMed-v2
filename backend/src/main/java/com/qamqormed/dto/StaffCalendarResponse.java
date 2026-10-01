package com.qamqormed.dto;

import java.time.LocalDate;
import java.util.List;

public record StaffCalendarResponse(Long doctorId, LocalDate from, LocalDate to, DoctorScheduleResponse schedule,
                                     List<ScheduleExceptionResponse> exceptions, List<CalendarEventResponse> events) {}
