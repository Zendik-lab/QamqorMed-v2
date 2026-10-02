package com.qamqormed.dto;

public record MedicalHistorySummaryResponse(
        long visits,
        long diagnoses,
        long activePrescriptions,
        long activeMedications,
        long pendingLabResults,
        long activeReferrals,
        long certificates,
        long documents
) {}
