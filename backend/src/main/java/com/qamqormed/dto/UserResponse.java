package com.qamqormed.dto;

import com.qamqormed.entity.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String role,
        SpecialtySummary specialty,
        ClinicSummary clinic
) {
    public record SpecialtySummary(Long id, String name) {}

    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                null,
                null
        );
    }

    public static UserResponse from(User user, SpecialtySummary specialty, ClinicSummary clinic) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                specialty,
                clinic
        );
    }
}
