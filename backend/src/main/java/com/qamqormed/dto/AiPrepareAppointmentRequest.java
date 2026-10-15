package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

public record AiPrepareAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull Long timeSlotId,
        Long dependentId
) {}
