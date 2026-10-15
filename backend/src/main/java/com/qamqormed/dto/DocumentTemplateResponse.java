package com.qamqormed.dto;

import com.qamqormed.entity.DocumentTemplate;

public record DocumentTemplateResponse(Long id, String name, String documentType, String scope, String language,
                                       Long specialtyId, String specialtyName, Long clinicId, Long ownerDoctorId,
                                       Integer currentVersion, String content, String requiredFields,
                                       boolean favorite, boolean defaultTemplate, boolean active) {
    public static DocumentTemplateResponse from(DocumentTemplate value, boolean favorite, boolean defaultTemplate) {
        var version = value.getCurrentVersion();
        return new DocumentTemplateResponse(value.getId(), value.getName(), value.getDocumentType().name(),
                value.getScope().name(), value.getLanguage().name(),
                value.getSpecialty() == null ? null : value.getSpecialty().getId(),
                value.getSpecialty() == null ? null : value.getSpecialty().getName(),
                value.getClinic() == null ? null : value.getClinic().getId(),
                value.getOwnerDoctor() == null ? null : value.getOwnerDoctor().getId(),
                version == null ? null : version.getVersionNumber(),
                version == null ? null : version.getContent(),
                version == null ? null : version.getRequiredFields(), favorite, defaultTemplate, value.isActive());
    }
}
