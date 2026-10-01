package com.qamqormed.dto;

import com.qamqormed.entity.Treatment;

import java.time.Instant;

public record TreatmentResponse(Long id, String name, String dosage, String frequency, String duration,
                                String instructions, Instant createdAt) {
    public static TreatmentResponse from(Treatment treatment) {
        return new TreatmentResponse(treatment.getId(), treatment.getName(), treatment.getDosage(),
                treatment.getFrequency(), treatment.getDuration(), treatment.getInstructions(), treatment.getCreatedAt());
    }
}
