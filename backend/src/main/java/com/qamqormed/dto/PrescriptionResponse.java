package com.qamqormed.dto;

import com.qamqormed.entity.Prescription;
import java.time.Instant;
import java.time.LocalDate;

public record PrescriptionResponse(Long id, Long medicalVisitId, String title, String description, String instructions, LocalDate startDate, LocalDate endDate, Instant createdAt) {
    public static PrescriptionResponse from(Prescription value) {
        return new PrescriptionResponse(value.getId(), value.getMedicalVisit().getId(), value.getTitle(), value.getDescription(), value.getInstructions(), value.getStartDate(), value.getEndDate(), value.getCreatedAt());
    }
}
