package com.qamqormed.dto;
import com.qamqormed.entity.Region;
public record RegionResponse(Long id, String name, String code, boolean active) {
    public static RegionResponse from(Region region) { return new RegionResponse(region.getId(), region.getName(), region.getCode(), region.isActive()); }
}
