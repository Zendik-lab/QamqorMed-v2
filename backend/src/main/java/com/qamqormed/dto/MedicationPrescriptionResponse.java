package com.qamqormed.dto;

import com.qamqormed.entity.MedicationPrescription;
import java.time.Instant;
import java.time.LocalDate;

public record MedicationPrescriptionResponse(Long id, Long medicalVisitId, String medicationName, String dosage, String frequency, String duration, String instructions, LocalDate startDate, LocalDate endDate, Instant createdAt) {
    public static MedicationPrescriptionResponse from(MedicationPrescription value) {
        return new MedicationPrescriptionResponse(value.getId(), value.getMedicalVisit().getId(), value.getMedicationName(), value.getDosage(), value.getFrequency(), value.getDuration(), value.getInstructions(), value.getStartDate(), value.getEndDate(), value.getCreatedAt());
    }
}
