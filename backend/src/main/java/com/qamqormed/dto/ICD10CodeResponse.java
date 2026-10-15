package com.qamqormed.dto;

import com.qamqormed.entity.ICD10Code;

public record ICD10CodeResponse(Long id, String code, String name, String titleRu, String titleKk, String titleEn,
                                String description, String chapter, String parentCode, boolean selectable,
                                String source, String sourceVersion, boolean active) {
    public static ICD10CodeResponse from(ICD10Code value) {
        return new ICD10CodeResponse(value.getId(), value.getCode(), value.getName(), value.getTitleRu(),
                value.getTitleKk(), value.getTitleEn(), value.getDescription(), value.getChapter(),
                value.getParentCode(), value.isSelectable(), value.getSource(), value.getSourceVersion(), value.isActive());
    }
}
