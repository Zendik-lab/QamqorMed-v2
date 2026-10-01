package com.qamqormed.dto;
import com.qamqormed.entity.NurseProfile;
public record NurseResponse(Long id, Long userId, String firstName, String lastName, String middleName, String phone,
                            ClinicSummary clinic, boolean demo, boolean active, DoctorSummary doctor,
                            String avatarUrl, String avatar, String cabinetNumber, String education, String qualification,
                            int experienceYears, String about, java.time.Instant createdAt, java.time.Instant updatedAt) {
    public record ClinicSummary(Long id, String name, String cityName, String regionName, String address) {}
    public record DoctorSummary(Long id, Long userId, String firstName, String lastName, String middleName,
                                String avatarUrl, String specialty, String cabinetNumber, String clinicName) {}
    public static NurseResponse from(NurseProfile profile) {
        return from(profile, null);
    }
    public static NurseResponse from(NurseProfile profile, com.qamqormed.entity.DoctorProfile doctorProfile) {
        var user = profile.getUser(); var clinic = profile.getClinic(); var city = clinic.getCity();
        DoctorSummary doctor = doctorProfile == null ? null : new DoctorSummary(doctorProfile.getId(), doctorProfile.getUser().getId(), doctorProfile.getUser().getFirstName(), doctorProfile.getUser().getLastName(), doctorProfile.getUser().getMiddleName(), doctorProfile.getAvatarUrl(), doctorProfile.getSpecialty().getName(), doctorProfile.getCabinetNumber(), doctorProfile.getClinic().getName());
        return new NurseResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(), user.getMiddleName(), user.getPhone(),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName(), clinic.getAddress()), profile.isDemo(), profile.isActive(), doctor,
                profile.getAvatarUrl(), profile.getAvatarUrl(), profile.getCabinetNumber(), profile.getEducation(), profile.getQualification(), profile.getExperienceYears(), profile.getAbout(), profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
