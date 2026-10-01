package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

public record DoctorNursePairRequest(@NotNull Long doctorId, @NotNull Long nurseId) {}
