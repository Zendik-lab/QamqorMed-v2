package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AiPrepareWaitlistRequest(
        Long dependentId,
        @NotNull Long clinicId,
        @NotNull Long specialtyId,
        Long doctorId,
        @NotNull LocalDate desiredDateFrom,
        @NotNull LocalDate desiredDateTo,
        LocalTime preferredTimeFrom,
        LocalTime preferredTimeTo
) {}
