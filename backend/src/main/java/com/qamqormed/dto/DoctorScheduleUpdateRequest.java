package com.qamqormed.dto;

import java.util.List;

public record DoctorScheduleUpdateRequest(Integer slotDurationMinutes,
                                          List<ScheduleIntervalRequest> intervals,
                                          List<ScheduleExceptionRequest> exceptions) {}
