package com.qamqormed.dto;

import com.qamqormed.entity.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DependentRequest(
        @NotBlank(message = "Укажите имя") String firstName,
        @NotBlank(message = "Укажите фамилию") String lastName,
        String middleName,
        @NotNull(message = "Укажите дату рождения") LocalDate dateOfBirth,
        String gender,
        String iin,
        @NotNull(message = "Укажите родство") RelationshipType relationship,
        String phone
) {
}
