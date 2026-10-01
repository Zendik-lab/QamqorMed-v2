package com.qamqormed.dto;

import com.qamqormed.entity.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String role,
        SpecialtySummary specialty,
        ClinicSummary clinic,
        LocationSummary preferredRegion,
        LocationSummary preferredCity
) {
    public record SpecialtySummary(Long id, String name) {}

    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}
    public record LocationSummary(Long id, String name) {}

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                null,
                null,
                region(user),
                city(user)
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
                clinic,
                region(user),
                city(user)
        );
    }

    private static LocationSummary region(User user) {
        return user.getPreferredRegion() == null ? null : new LocationSummary(user.getPreferredRegion().getId(), user.getPreferredRegion().getName());
    }

    private static LocationSummary city(User user) {
        return user.getPreferredCity() == null ? null : new LocationSummary(user.getPreferredCity().getId(), user.getPreferredCity().getName());
    }
}
