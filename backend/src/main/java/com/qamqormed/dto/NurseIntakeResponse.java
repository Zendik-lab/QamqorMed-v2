package com.qamqormed.dto;

import com.qamqormed.entity.NurseIntake;

import java.math.BigDecimal;
import java.time.Instant;

public record NurseIntakeResponse(
        Long id,
        Long appointmentId,
        MedicalSubjectDto subject,
        BigDecimal temperature,
        String bloodPressure,
        Integer pulse,
        BigDecimal weight,
        BigDecimal height,
        String notes,
        Instant createdAt
) {
    public static NurseIntakeResponse from(NurseIntake intake) {
        var appointment = intake.getAppointment();
        return new NurseIntakeResponse(intake.getId(), appointment.getId(),
                appointment.getDependent() == null ? MedicalSubjectDto.self(appointment.getBookedBy()) : MedicalSubjectDto.dependent(appointment.getDependent()),
                intake.getTemperature(), intake.getBloodPressure(), intake.getPulse(), intake.getWeight(), intake.getHeight(), intake.getNotes(), intake.getCreatedAt());
    }
}
