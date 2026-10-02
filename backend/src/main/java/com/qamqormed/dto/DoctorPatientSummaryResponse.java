package com.qamqormed.dto;

import java.time.LocalDate;

/** A patient summary is built only from appointments belonging to the authenticated doctor. */
public record DoctorPatientSummaryResponse(
        MedicalSubjectDto subject,
        String phone,
        String email,
        String ownerFullName,
        Integer appointmentCount,
        LocalDate lastAppointmentDate,
        LocalDate nextAppointmentDate,
        String lastDiagnosis
) {}
