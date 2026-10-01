package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

public record VisitDiagnosisRequest(@NotNull Long icd10CodeId, String notes, boolean primary) {}
