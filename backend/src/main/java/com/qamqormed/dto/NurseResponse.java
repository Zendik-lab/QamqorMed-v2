package com.qamqormed.dto;
import com.qamqormed.entity.NurseProfile;
public record NurseResponse(Long id, Long userId, String firstName, String lastName, String phone,
                            ClinicSummary clinic, boolean active) {
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}
    public static NurseResponse from(NurseProfile profile) {
        var user = profile.getUser(); var clinic = profile.getClinic(); var city = clinic.getCity();
        return new NurseResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(), user.getPhone(),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()), profile.isActive());
    }
}
