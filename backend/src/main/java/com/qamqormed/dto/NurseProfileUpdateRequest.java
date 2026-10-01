package com.qamqormed.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record NurseProfileUpdateRequest(
        @Size(max = 500) String avatarUrl,
        @Size(max = 30) String cabinetNumber,
        @Size(max = 2000) String education,
        @Size(max = 300) String qualification,
        @Min(0) @Max(70) Integer experienceYears,
        @Size(max = 4000) String about
) {}
