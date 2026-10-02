package com.qamqormed.dto;

import java.util.List;

public record MedicalHistoryResponse(
        MedicalSubjectDto subject,
        MedicalHistorySummaryResponse summary,
        List<MedicalTimelineItemResponse> timeline,
        List<MedicalVisitResponse> visits
) {}
