package com.qamqormed.dto;

public record MedicalVisitUpsertRequest(
        String chiefComplaint,
        String anamnesis,
        String diagnosis,
        String diagnosisCode,
        String diagnosisName,
        String doctorNotes,
        String recommendations
) {
}
