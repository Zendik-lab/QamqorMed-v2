package com.qamqormed.dto;
import com.qamqormed.entity.NurseProfile;
public record NurseResponse(Long id, Long userId, String firstName, String lastName, String middleName, String phone,
                            ClinicSummary clinic, boolean demo, boolean active, DoctorSummary doctor) {
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}
    public record DoctorSummary(Long id, String firstName, String lastName) {}
    public static NurseResponse from(NurseProfile profile) {
        return from(profile, null);
    }
    public static NurseResponse from(NurseProfile profile, com.qamqormed.entity.DoctorProfile doctorProfile) {
        var user = profile.getUser(); var clinic = profile.getClinic(); var city = clinic.getCity();
        DoctorSummary doctor = doctorProfile == null ? null : new DoctorSummary(doctorProfile.getId(), doctorProfile.getUser().getFirstName(), doctorProfile.getUser().getLastName());
        return new NurseResponse(profile.getId(), user.getId(), user.getFirstName(), user.getLastName(), user.getMiddleName(), user.getPhone(),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()), profile.isDemo(), profile.isActive(), doctor);
    }
}
