package com.qamqormed.dto;

import com.qamqormed.entity.VisitDiagnosis;
import java.time.Instant;

public record VisitDiagnosisResponse(Long id, Long medicalVisitId, ICD10CodeResponse icd10Code, String notes, boolean primary, Instant createdAt) {
    public static VisitDiagnosisResponse from(VisitDiagnosis value) {
        return new VisitDiagnosisResponse(value.getId(), value.getMedicalVisit().getId(), ICD10CodeResponse.from(value.getIcd10Code()), value.getNotes(), value.isPrimaryDiagnosis(), value.getCreatedAt());
    }
}
