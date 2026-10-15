package com.qamqormed.dto;

import com.qamqormed.entity.MedicalDocumentType;
import com.qamqormed.entity.TemplateLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentTemplateCreateRequest(@NotBlank String name, @NotNull MedicalDocumentType documentType,
                                            @NotNull TemplateLanguage language, Long specialtyId,
                                            String content, String requiredFields) {}
