package com.qamqormed.dto;

import com.qamqormed.entity.ICD10Code;

public record ICD10CodeResponse(Long id, String code, String name, String description, boolean active) {
    public static ICD10CodeResponse from(ICD10Code value) {
        return new ICD10CodeResponse(value.getId(), value.getCode(), value.getName(), value.getDescription(), value.isActive());
    }
}
