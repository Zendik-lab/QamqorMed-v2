package com.qamqormed.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record NurseIntakeRequest(
        @DecimalMin(value = "0.0", inclusive = false, message = "Температура должна быть положительной") BigDecimal temperature,
        String bloodPressure,
        @Min(value = 0, message = "Пульс не может быть отрицательным") Integer pulse,
        @DecimalMin(value = "0.0", inclusive = false, message = "Вес должен быть положительным") BigDecimal weight,
        @DecimalMin(value = "0.0", inclusive = false, message = "Рост должен быть положительным") BigDecimal height,
        String notes
) {
}
