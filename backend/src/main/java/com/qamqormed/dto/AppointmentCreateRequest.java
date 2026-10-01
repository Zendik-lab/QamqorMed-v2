package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull(message = "Выберите врача") Long doctorId,
        @NotNull(message = "Выберите дату и время") LocalDateTime appointmentAt,
        Long dependentId,
        String note,
        Long cityId,
        Long specialtyId,
        Long clinicId,
        Long timeSlotId
) {
}
