package com.qamqormed.dto;

import com.qamqormed.entity.DoctorSchedule;
import java.util.List;

public record DoctorScheduleResponse(Long doctorId, int slotDurationMinutes, String timeZone,
                                     List<ScheduleIntervalResponse> intervals,
                                     List<ScheduleExceptionResponse> exceptions) {
    public static DoctorScheduleResponse from(DoctorSchedule schedule) {
        return new DoctorScheduleResponse(schedule.getDoctor().getId(), schedule.getSlotDurationMinutes(), schedule.getTimeZone(),
                schedule.getIntervals().stream().map(ScheduleIntervalResponse::from).toList(),
                schedule.getExceptions().stream().map(ScheduleExceptionResponse::from).toList());
    }
}
