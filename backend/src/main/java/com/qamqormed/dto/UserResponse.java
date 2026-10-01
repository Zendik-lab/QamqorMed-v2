package com.qamqormed.dto;

import com.qamqormed.entity.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
