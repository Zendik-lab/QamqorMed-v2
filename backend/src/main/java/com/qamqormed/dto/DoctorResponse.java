package com.qamqormed.dto;
import com.qamqormed.entity.DoctorProfile;

public record DoctorResponse(Long id, Long userId, String firstName, String lastName, String middleName, String phone,
                             SpecialtySummary specialty, ClinicSummary clinic, String cityName, String regionName,
                             int experienceYears, String bio, String officeNumber, boolean demo, boolean active,
                             NurseSummary nurse) {
    public record SpecialtySummary(Long id, String code, String name, String nameRu, String nameKk, String nameEn) {}
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}
    public record NurseSummary(Long id, String firstName, String lastName, boolean demo) {}
    public static DoctorResponse from(DoctorProfile profile) {
        return from(profile, null);
    }
    public static DoctorResponse from(DoctorProfile profile, com.qamqormed.entity.NurseProfile nurseProfile) {
        var user = profile.getUser();
        var clinic = profile.getClinic();
        var city = clinic.getCity();
        NurseSummary nurse = nurseProfile == null ? null : new NurseSummary(nurseProfile.getId(), nurseProfile.getUser().getFirstName(), nurseProfile.getUser().getLastName(), nurseProfile.isDemo());
        return new DoctorResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(), user.getMiddleName(), user.getPhone(),
                new SpecialtySummary(profile.getSpecialty().getId(), profile.getSpecialty().getCode(), profile.getSpecialty().getName(),
                        profile.getSpecialty().getNameRu(), profile.getSpecialty().getNameKk(), profile.getSpecialty().getNameEn()),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()),
                city.getName(), city.getRegion().getName(), profile.getExperienceYears(), profile.getBio(),
                profile.getOfficeNumber(), profile.isDemo(), profile.isActive(), nurse);
    }
}
