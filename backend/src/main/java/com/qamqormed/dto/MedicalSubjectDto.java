package com.qamqormed.dto;

import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.User;

import java.time.LocalDate;
import java.time.Period;

public record MedicalSubjectDto(
        String type,
        Long id,
        String fullName,
        LocalDate dateOfBirth,
        Integer age,
        String gender,
        String relationship,
        Long dependentId
) {
    public static MedicalSubjectDto self(User user) {
        return new MedicalSubjectDto("SELF", user.getId(), fullName(user.getLastName(), user.getFirstName(), user.getMiddleName()),
                user.getDateOfBirth(), age(user.getDateOfBirth()), user.getGender(), null, null);
    }

    public static MedicalSubjectDto dependent(Dependent dependent) {
        return new MedicalSubjectDto("DEPENDENT", dependent.getId(), fullName(dependent.getLastName(), dependent.getFirstName(), dependent.getMiddleName()),
                dependent.getDateOfBirth(), age(dependent.getDateOfBirth()), dependent.getGender(), dependent.getRelationship().name(), dependent.getId());
    }

    private static String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }

    private static Integer age(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
