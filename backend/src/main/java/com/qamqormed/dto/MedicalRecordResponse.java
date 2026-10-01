package com.qamqormed.dto;

import com.qamqormed.entity.MedicalRecord;

import java.time.Instant;

public record MedicalRecordResponse(
        Long id,
        Long appointmentId,
        MedicalSubjectDto subject,
        AppointmentResponse.StaffSummary doctor,
        String diagnosis,
        String recommendations,
        String prescriptions,
        String referrals,
        Instant createdAt
) {
    public static MedicalRecordResponse from(MedicalRecord record) {
        var doctor = record.getAppointment().getDoctor();
        var doctorUser = doctor.getUser();
        return new MedicalRecordResponse(record.getId(), record.getAppointment().getId(),
                record.getDependent() == null ? MedicalSubjectDto.self(record.getPatientUser()) : MedicalSubjectDto.dependent(record.getDependent()),
                new AppointmentResponse.StaffSummary(doctor.getId(), doctorUser.getId(),
                        String.join(" ", doctorUser.getLastName(), doctorUser.getFirstName(), doctorUser.getMiddleName() == null ? "" : doctorUser.getMiddleName()).trim().replaceAll(" +", " "),
                        doctor.getSpecialty().getName()), record.getDiagnosis(), record.getRecommendations(), record.getPrescriptions(), record.getReferrals(), record.getCreatedAt());
    }
}
