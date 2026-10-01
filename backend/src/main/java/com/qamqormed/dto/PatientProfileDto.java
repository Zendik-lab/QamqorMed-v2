package com.qamqormed.dto;

import com.qamqormed.entity.User;

import java.time.LocalDate;
import java.time.Period;

public record PatientProfileDto(
        Long id,
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
        LocationSummary preferredCity
) {
    public record LocationSummary(Long id, String name) {}

    public static PatientProfileDto from(User user) {
        return new PatientProfileDto(user.getId(), user.getFirstName(), user.getLastName(), user.getMiddleName(),
                user.getDateOfBirth(), age(user.getDateOfBirth()), user.getGender(), user.getIin(), user.getPhone(), user.getEmail(),
                user.getPreferredRegion() == null ? null : new LocationSummary(user.getPreferredRegion().getId(), user.getPreferredRegion().getName()),
                user.getPreferredCity() == null ? null : new LocationSummary(user.getPreferredCity().getId(), user.getPreferredCity().getName()));
    }

    private static Integer age(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
