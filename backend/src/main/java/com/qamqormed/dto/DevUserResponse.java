package com.qamqormed.dto;

import java.util.List;

public record DevUserResponse(
        Long userId,
        String role,
        String fullName,
        String phone,
        boolean demo,
        boolean active,
        Long doctorProfileId,
        Long nurseProfileId,
        DevNamedValue clinic,
        DevNamedValue city,
        DevNamedValue region,
        DevSpecialtyValue specialty,
        DevLoginCredentials credentials,
        DevRelationResponse relation,
        List<String> warnings
) {
}
