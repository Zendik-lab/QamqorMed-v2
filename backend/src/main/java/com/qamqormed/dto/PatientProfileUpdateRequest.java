package com.qamqormed.dto;

import java.time.LocalDate;

public record PatientProfileUpdateRequest(
        String firstName,
        String lastName,
        String middleName,
        LocalDate dateOfBirth,
        String gender,
        String iin,
        String email
) {
}
