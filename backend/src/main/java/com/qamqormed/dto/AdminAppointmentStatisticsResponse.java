package com.qamqormed.dto;
public record AdminAppointmentStatisticsResponse(long total, long completed, long cancelled, long noShow,
                                                 long confirmed, long pendingConfirmation, int noShowRate) {}
