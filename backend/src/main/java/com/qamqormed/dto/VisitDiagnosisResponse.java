package com.qamqormed.dto;

import com.qamqormed.entity.VisitDiagnosis;
import java.time.Instant;

public record VisitDiagnosisResponse(Long id, Long medicalVisitId, ICD10CodeResponse icd10Code, String notes, boolean primary,
                                     String diagnosisType, String status, Instant createdAt, Instant updatedAt) {
    public static VisitDiagnosisResponse from(VisitDiagnosis value) {
        return new VisitDiagnosisResponse(value.getId(), value.getMedicalVisit().getId(), ICD10CodeResponse.from(value.getIcd10Code()), value.getNotes(), value.isPrimaryDiagnosis(),
                value.getDiagnosisType().name(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
