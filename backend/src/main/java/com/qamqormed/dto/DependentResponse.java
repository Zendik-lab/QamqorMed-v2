package com.qamqormed.dto;

import com.qamqormed.entity.Dependent;

import java.time.LocalDate;
import java.time.Period;

public record DependentResponse(
        Long id,
        Long ownerId,
        String firstName,
        String lastName,
        String middleName,
        String fullName,
        LocalDate dateOfBirth,
        Integer age,
        String gender,
        String iin,
        String relationship,
        String relationshipLabel,
        String phone,
        boolean active
) {
    public static DependentResponse from(Dependent dependent) {
        String fullName = String.join(" ", dependent.getLastName(), dependent.getFirstName(),
                dependent.getMiddleName() == null ? "" : dependent.getMiddleName()).trim().replaceAll(" +", " ");
        return new DependentResponse(dependent.getId(), dependent.getOwner().getId(), dependent.getFirstName(), dependent.getLastName(),
                dependent.getMiddleName(), fullName, dependent.getDateOfBirth(), age(dependent.getDateOfBirth()), dependent.getGender(),
                dependent.getIin(), dependent.getRelationship().name(), relationshipLabel(dependent), dependent.getPhone(), dependent.isActive());
    }

    private static Integer age(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public static String relationshipLabel(Dependent dependent) {
        return switch (dependent.getRelationship()) {
            case CHILD -> "Ребёнок";
            case SPOUSE -> "Супруг / супруга";
            case PARENT -> "Родитель";
            case SIBLING -> "Брат / сестра";
            case GUARDIAN -> "Подопечный";
            case OTHER -> "Другой подопечный";
        };
    }
}
