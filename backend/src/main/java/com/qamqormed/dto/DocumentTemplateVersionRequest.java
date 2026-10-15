package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentTemplateVersionRequest(@NotBlank String content, String requiredFields,
                                             String changeDescription) {}
