package com.qamqormed.dto;

import com.qamqormed.entity.VisitDiagnosis;
import java.time.Instant;

public record VisitDiagnosisResponse(Long id, Long medicalVisitId, ICD10CodeResponse icd10Code, String notes, boolean primary,
                                     String diagnosisType, String status, Instant createdAt, Instant updatedAt) {
    public static VisitDiagnosisResponse from(VisitDiagnosis value) {
        ICD10CodeResponse code = ICD10CodeResponse.from(value.getIcd10Code());
        if (value.getIcd10CodeSnapshot() != null || value.getIcd10NameSnapshot() != null) {
            code = new ICD10CodeResponse(code.id(), value.getIcd10CodeSnapshot(),
                    value.getIcd10NameSnapshot(), code.titleRu(), code.titleKk(), code.titleEn(), code.description(),
                    code.chapter(), code.parentCode(), code.selectable(), code.source(), code.sourceVersion(), code.active());
        }
        return new VisitDiagnosisResponse(value.getId(), value.getMedicalVisit().getId(), code, value.getNotes(), value.isPrimaryDiagnosis(),
                value.getDiagnosisType().name(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
