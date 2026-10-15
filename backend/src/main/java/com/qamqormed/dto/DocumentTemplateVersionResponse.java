package com.qamqormed.dto;

import com.qamqormed.entity.DocumentTemplateVersion;

import java.time.Instant;

public record DocumentTemplateVersionResponse(Long id, Long templateId, int versionNumber, String content,
                                              String requiredFields, String changeDescription, Instant createdAt) {
    public static DocumentTemplateVersionResponse from(DocumentTemplateVersion value) {
        return new DocumentTemplateVersionResponse(value.getId(), value.getTemplate().getId(), value.getVersionNumber(),
                value.getContent(), value.getRequiredFields(), value.getChangeDescription(), value.getCreatedAt());
    }
}
