package com.qamqormed.dto;
public record AttendanceResponse(long totalAppointments, long completedAppointments, long cancelledAppointments,
                                 long noShowAppointments, int attendanceRate) {}
