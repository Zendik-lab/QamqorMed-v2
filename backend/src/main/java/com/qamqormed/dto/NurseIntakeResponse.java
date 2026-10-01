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
        Integer bloodPressureSystolic,
        Integer bloodPressureDiastolic,
        Integer pulse,
        Integer oxygenSaturation,
        BigDecimal weight,
        BigDecimal height,
        String complaintSummary,
        String nurseNotes,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static NurseIntakeResponse from(NurseIntake intake) {
        var appointment = intake.getAppointment();
        return new NurseIntakeResponse(intake.getId(), appointment.getId(),
                appointment.getDependent() == null ? MedicalSubjectDto.self(appointment.getBookedBy()) : MedicalSubjectDto.dependent(appointment.getDependent()),
                intake.getTemperature(), intake.getBloodPressure(), intake.getBloodPressureSystolic(), intake.getBloodPressureDiastolic(),
                intake.getPulse(), intake.getOxygenSaturation(), intake.getWeight(), intake.getHeight(), intake.getComplaintSummary(),
                intake.getNurseNotes(), intake.getNotes(), intake.getCreatedAt(), intake.getUpdatedAt());
    }
}
