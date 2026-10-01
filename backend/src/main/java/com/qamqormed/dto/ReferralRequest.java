package com.qamqormed.dto;

import com.qamqormed.entity.ReferralType;

public record ReferralRequest(
        ReferralType referralType,
        Long targetSpecialtyId,
        Long targetClinicId,
        String reason,
        String notes
) {
}
