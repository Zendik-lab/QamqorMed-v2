package com.qamqormed.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record WaitlistBookRequest(@NotNull Long doctorId, @NotNull LocalDateTime appointmentAt, Long timeSlotId) {}
