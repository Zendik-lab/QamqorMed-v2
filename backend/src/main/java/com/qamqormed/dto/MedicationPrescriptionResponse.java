package com.qamqormed.dto;

import com.qamqormed.entity.MedicationPrescription;
import java.time.Instant;
import java.time.LocalDate;

public record MedicationPrescriptionResponse(Long id, Long medicalVisitId, String medicationName, String dosage, String dosageUnit, String frequency, String route,
                                             String duration, Integer durationDays, String instructions, LocalDate startDate, LocalDate endDate,
                                             String status, Instant createdAt, Instant updatedAt) {
    public static MedicationPrescriptionResponse from(MedicationPrescription value) {
        return new MedicationPrescriptionResponse(value.getId(), value.getMedicalVisit().getId(), value.getMedicationName(), value.getDosage(), value.getDosageUnit(), value.getFrequency(), value.getRoute(), value.getDuration(), value.getDurationDays(), value.getInstructions(), value.getStartDate(), value.getEndDate(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
