package com.qamqormed.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DoctorProfileUpdateRequest(
        @Size(max = 500) String avatarUrl,
        @Size(max = 4000) String about,
        @Size(max = 2000) String education,
        @Size(max = 120) String qualificationCategory,
        @Size(max = 30) String cabinetNumber,
        @Min(10) @Max(240) Integer consultationDurationMinutes
) {}
