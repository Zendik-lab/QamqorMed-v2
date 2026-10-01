package com.qamqormed.dto;
import com.qamqormed.entity.DoctorProfile;

public record DoctorResponse(Long id, Long userId, String firstName, String lastName, String phone,
                             SpecialtySummary specialty, ClinicSummary clinic, String cityName, String regionName,
                             int experienceYears, String bio, String officeNumber, boolean active) {
    public record SpecialtySummary(Long id, String name) {}
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}
    public static DoctorResponse from(DoctorProfile profile) {
        var user = profile.getUser();
        var clinic = profile.getClinic();
        var city = clinic.getCity();
        return new DoctorResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(), user.getPhone(),
                new SpecialtySummary(profile.getSpecialty().getId(), profile.getSpecialty().getName()),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()),
                city.getName(), city.getRegion().getName(), profile.getExperienceYears(), profile.getBio(),
                profile.getOfficeNumber(), profile.isActive());
    }
}
