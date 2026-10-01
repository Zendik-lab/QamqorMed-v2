package com.qamqormed.dto;

import com.qamqormed.entity.Referral;

import java.time.Instant;

public record ReferralResponse(Long id, String referralType, Long targetSpecialtyId, String targetSpecialtyName,
                               Long targetClinicId, String targetClinicName, String reason, String notes,
                               String status, Instant createdAt) {
    public static ReferralResponse from(Referral referral) {
        return new ReferralResponse(referral.getId(), referral.getReferralType().name(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getId(),
                referral.getTargetSpecialty() == null ? null : referral.getTargetSpecialty().getName(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getId(),
                referral.getTargetClinic() == null ? null : referral.getTargetClinic().getName(),
                referral.getReason(), referral.getNotes(), referral.getStatus().name(), referral.getCreatedAt());
    }
}
