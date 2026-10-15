package com.qamqormed.dto;

import com.qamqormed.entity.Referral;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read model for the doctor's referral workspace. The appointment remains the source of truth. */
public record DoctorReferralResponse(
        Long id,
        Long appointmentId,
        MedicalSubjectDto subject,
        LocalDateTime appointmentAt,
        String referralType,
        Long targetSpecialtyId,
        String targetSpecialtyName,
        Long targetClinicId,
        String targetClinicName,
        Long targetDoctorId,
        String targetDoctorName,
        String reason,
        String notes,
        String status,
        Instant issuedAt,
        LocalDate validUntil,
        Instant createdAt,
        Long medicalVisitId
) {
    public static DoctorReferralResponse from(Referral referral) {
        var visit = referral.getMedicalVisit();
        var appointment = visit.getAppointment();
        var appointmentSummary = AppointmentResponse.from(appointment);
        return new DoctorReferralResponse(
                referral.getId(), appointment.getId(), appointmentSummary.subject(), appointment.getAppointmentAt(),
                referral.getReferralType().name(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getId(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getName(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getId(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getName(),
                referral.getTargetDoctor() == null ? null : referral.getTargetDoctor().getId(),
                referral.getTargetDoctor() == null ? null : fullName(referral.getTargetDoctor().getUser()),
                referral.getReason(), referral.getNotes(), referral.getStatus().name(), referral.getIssuedAt(),
                referral.getValidUntil(), referral.getCreatedAt(), visit.getId());
    }

    private static String fullName(com.qamqormed.entity.User user) {
        return String.join(" ", user.getLastName(), user.getFirstName(), user.getMiddleName() == null ? "" : user.getMiddleName())
                .trim().replaceAll(" +", " ");
    }
}
