package com.qamqormed.dto;

import com.qamqormed.entity.Referral;

import java.time.Instant;

public record ReferralResponse(Long id, String referralType, Long targetSpecialtyId, String targetSpecialtyName,
                               Long targetClinicId, String targetClinicName, Long targetDoctorId, String targetDoctorName, String reason, String notes,
                               String status, Instant issuedAt, java.time.LocalDate validUntil, Instant createdAt, Long medicalVisitId) {
    public static ReferralResponse from(Referral referral) {
        return new ReferralResponse(referral.getId(), referral.getReferralType().name(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getId(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getName(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getId(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getName(),
                referral.getTargetDoctor() == null ? null : referral.getTargetDoctor().getId(),
                referral.getTargetDoctor() == null ? null : referral.getTargetDoctor().getUser().getLastName() + " " + referral.getTargetDoctor().getUser().getFirstName(),
                referral.getReason(), referral.getNotes(), referral.getStatus().name(), referral.getIssuedAt(), referral.getValidUntil(), referral.getCreatedAt(),
                referral.getMedicalVisit() == null ? null : referral.getMedicalVisit().getId());
    }
}
