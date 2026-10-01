package com.qamqormed.dto;
import com.qamqormed.entity.City;
public record CityResponse(Long id, String name, String code, String nameRu, String nameKk, String nameEn,
                           Long regionId, String regionName, boolean active) {
    public static CityResponse from(City city) { return new CityResponse(city.getId(), city.getName(), city.getCode(),
            city.getNameRu(), city.getNameKk(), city.getNameEn(), city.getRegion().getId(), city.getRegion().getName(), city.isActive()); }
}
