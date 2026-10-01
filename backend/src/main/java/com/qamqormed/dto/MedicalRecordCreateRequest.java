package com.qamqormed.dto;

public record MedicalRecordCreateRequest(
        String diagnosis,
        String recommendations,
        String prescriptions,
        String referrals
) {
}
