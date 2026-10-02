package com.qamqormed.dto;

import com.qamqormed.entity.ReferralType;

public record ReferralRequest(
        ReferralType referralType,
        Long targetSpecialtyId,
        Long targetClinicId,
        Long targetDoctorId,
        String reason,
        String notes,
        java.time.LocalDate validUntil
) {
}
