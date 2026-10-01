package com.qamqormed.dto;

import java.util.List;

public record DevRelationResponse(
        Long doctorId,
        Long nurseId,
        Long doctorUserId,
        Long nurseUserId,
        Long clinicId,
        Long specialtyId,
        Long assignmentId,
        String doctorName,
        String nurseName,
        String specialtyName,
        String clinicName,
        String cityName,
        String regionName,
        boolean connected,
        List<String> warnings
) {
}
