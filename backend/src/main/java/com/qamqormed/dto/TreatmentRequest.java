package com.qamqormed.dto;

public record TreatmentRequest(
        String name,
        String dosage,
        String frequency,
        String duration,
        String instructions
) {
}
