package com.qamqormed.dto;

import com.qamqormed.entity.MedicalVisit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record MedicalVisitResponse(
        Long id,
        Long appointmentId,
        MedicalSubjectDto subject,
        AppointmentResponse.StaffSummary doctor,
        AppointmentResponse.ClinicSummary clinic,
        LocalDateTime appointmentAt,
        String appointmentStatus,
        String chiefComplaint,
        String anamnesis,
        String diagnosis,
        String diagnosisCode,
        String diagnosisName,
        String doctorNotes,
        String recommendations,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TreatmentResponse> treatments,
        List<ReferralResponse> referrals,
        List<MedicalDocumentResponse> documents,
        List<VisitDiagnosisResponse> diagnoses,
        List<PrescriptionResponse> prescriptions,
        List<MedicationPrescriptionResponse> medications
) {
    public static MedicalVisitResponse from(MedicalVisit visit,
                                             List<TreatmentResponse> treatments,
                                             List<ReferralResponse> referrals,
                                             List<MedicalDocumentResponse> documents) {
        return from(visit, treatments, referrals, documents, List.of(), List.of(), List.of());
    }

    public static MedicalVisitResponse from(MedicalVisit visit,
                                             List<TreatmentResponse> treatments,
                                             List<ReferralResponse> referrals,
                                             List<MedicalDocumentResponse> documents,
                                             List<VisitDiagnosisResponse> diagnoses,
                                             List<PrescriptionResponse> prescriptions,
                                             List<MedicationPrescriptionResponse> medications) {
        var doctor = visit.getAppointment().getDoctor();
        var doctorUser = doctor.getUser();
        return new MedicalVisitResponse(
                visit.getId(), visit.getAppointment().getId(),
                visit.getDependent() != null ? MedicalSubjectDto.dependent(visit.getDependent())
                        : visit.getPatientProfile() != null ? MedicalSubjectDto.self(visit.getPatientProfile()) : MedicalSubjectDto.self(visit.getPatientUser()),
                new AppointmentResponse.StaffSummary(doctor.getId(), doctorUser.getId(), fullName(doctorUser), doctor.getSpecialty().getName()),
                new AppointmentResponse.ClinicSummary(doctor.getClinic().getId(), doctor.getClinic().getName(),
                        doctor.getClinic().getCity().getName(), doctor.getClinic().getCity().getRegion().getName()),
                visit.getAppointment().getAppointmentAt(), visit.getAppointment().getCanonicalStatus().name(),
                visit.getChiefComplaint(), visit.getAnamnesis(), visit.getDiagnosis(), visit.getDiagnosisCode(),
                visit.getDiagnosisName(), visit.getDoctorNotes(), visit.getRecommendations(), visit.getStartedAt(),
                visit.getCompletedAt(), visit.getCreatedAt(), visit.getUpdatedAt(), treatments, referrals, documents, diagnoses, prescriptions, medications);
    }

    private static String fullName(com.qamqormed.entity.User user) {
        return String.join(" ", user.getLastName(), user.getFirstName(), user.getMiddleName() == null ? "" : user.getMiddleName())
                .trim().replaceAll(" +", " ");
    }
}
