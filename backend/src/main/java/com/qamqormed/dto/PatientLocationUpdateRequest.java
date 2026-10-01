package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

public record PatientLocationUpdateRequest(
        @NotNull(message = "Выберите область") Long regionId,
        @NotNull(message = "Выберите город") Long cityId
) {}
