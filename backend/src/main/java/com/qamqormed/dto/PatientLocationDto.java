package com.qamqormed.dto;

import com.qamqormed.entity.User;

public record PatientLocationDto(Long regionId, String regionName, Long cityId, String cityName) {
    public static PatientLocationDto from(User user) {
        return new PatientLocationDto(
                user.getPreferredRegion() == null ? null : user.getPreferredRegion().getId(),
                user.getPreferredRegion() == null ? null : user.getPreferredRegion().getName(),
                user.getPreferredCity() == null ? null : user.getPreferredCity().getId(),
                user.getPreferredCity() == null ? null : user.getPreferredCity().getName());
    }
}
