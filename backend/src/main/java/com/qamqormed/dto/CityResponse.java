package com.qamqormed.dto;
import com.qamqormed.entity.City;
public record CityResponse(Long id, String name, Long regionId, String regionName, boolean active) {
    public static CityResponse from(City city) { return new CityResponse(city.getId(), city.getName(), city.getRegion().getId(), city.getRegion().getName(), city.isActive()); }
}
