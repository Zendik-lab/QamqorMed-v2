package com.qamqormed.dto;

import com.qamqormed.entity.PatientProfile;
import com.qamqormed.entity.User;

import java.time.LocalDate;
import java.time.Period;

public record PatientProfileDto(
        Long id,
        Long userId,
        Long patientProfileId,
        String firstName,
        String lastName,
        String middleName,
        LocalDate dateOfBirth,
        Integer age,
        String gender,
        String iin,
        String phone,
        String email,
        LocationSummary preferredRegion,
        LocationSummary preferredCity,
        LocationSummary clinic,
        String address,
        String maskedIin
) {
    public record LocationSummary(Long id, String name) {}

    public static PatientProfileDto from(User user) {
        return new PatientProfileDto(user.getId(), user.getId(), null, user.getFirstName(), user.getLastName(), user.getMiddleName(),
                user.getDateOfBirth(), age(user.getDateOfBirth()), user.getGender(), user.getIin(), user.getPhone(), user.getEmail(),
                user.getPreferredRegion() == null ? null : new LocationSummary(user.getPreferredRegion().getId(), user.getPreferredRegion().getName()),
                user.getPreferredCity() == null ? null : new LocationSummary(user.getPreferredCity().getId(), user.getPreferredCity().getName()),
                null, null, mask(user.getIin()));
    }

    public static PatientProfileDto from(PatientProfile profile) {
        User user = profile.getUser();
        String gender = profile.getGender() == null ? null : profile.getGender().name();
        return new PatientProfileDto(user.getId(), user.getId(), profile.getId(), profile.getFirstName(), profile.getLastName(), profile.getMiddleName(),
                profile.getBirthDate(), age(profile.getBirthDate()), gender, profile.getIin(), profile.getPhone(), user.getEmail(),
                profile.getRegion() == null ? null : new LocationSummary(profile.getRegion().getId(), profile.getRegion().getName()),
                profile.getCity() == null ? null : new LocationSummary(profile.getCity().getId(), profile.getCity().getName()),
                profile.getClinic() == null ? null : new LocationSummary(profile.getClinic().getId(), profile.getClinic().getName()),
                profile.getAddress(), mask(profile.getIin()));
    }

    private static String mask(String iin) {
        if (iin == null || iin.length() < 4) return iin;
        return "********" + iin.substring(iin.length() - 4);
    }

    private static Integer age(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
