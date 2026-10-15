package com.qamqormed.dto;

import com.qamqormed.entity.MedicalDocumentType;
import com.qamqormed.entity.TemplateLanguage;
import com.qamqormed.entity.TemplateScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminDocumentTemplateRequest(@NotBlank String name, @NotNull MedicalDocumentType documentType,
                                           @NotNull TemplateScope scope, @NotNull TemplateLanguage language,
                                           Long clinicId, Long specialtyId, @NotBlank String content,
                                           String requiredFields) {}
