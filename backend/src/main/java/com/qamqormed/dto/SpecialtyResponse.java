package com.qamqormed.dto;
import com.qamqormed.entity.Specialty;
public record SpecialtyResponse(Long id, String name, String code, String nameRu, String nameKk, String nameEn,
                                String description, boolean active) {
    public static SpecialtyResponse from(Specialty specialty) { return new SpecialtyResponse(specialty.getId(), specialty.getName(),
            specialty.getCode(), specialty.getNameRu(), specialty.getNameKk(), specialty.getNameEn(), specialty.getDescription(), specialty.isActive()); }
}
