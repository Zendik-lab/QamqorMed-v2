package com.qamqormed.dto;

import java.time.Instant;

public record MedicalTimelineItemResponse(
        String type,
        Instant date,
        String title,
        String description,
        Long relatedEntityId,
        Long visitId
) {}
