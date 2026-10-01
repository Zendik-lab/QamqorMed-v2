package com.qamqormed.dto;
import com.qamqormed.entity.Clinic;
public record ClinicResponse(Long id, String name, Long cityId, String cityName, Long regionId, String regionName,
                             String address, String phone, String description, boolean active) {
    public static ClinicResponse from(Clinic clinic) {
        return new ClinicResponse(clinic.getId(), clinic.getName(), clinic.getCity().getId(), clinic.getCity().getName(),
                clinic.getCity().getRegion().getId(), clinic.getCity().getRegion().getName(), clinic.getAddress(),
                clinic.getPhone(), clinic.getDescription(), clinic.isActive());
    }
}
