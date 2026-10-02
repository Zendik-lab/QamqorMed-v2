package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record LabResultCreateRequest(
        @NotBlank String testName,
        String testCode,
        LocalDate testDate,
        @NotBlank String status,
        String result,
        String referenceRange,
        String comment
) {}
