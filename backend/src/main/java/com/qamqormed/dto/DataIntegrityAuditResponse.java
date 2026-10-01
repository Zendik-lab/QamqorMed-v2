package com.qamqormed.dto;

public record DataIntegrityAuditResponse(
        long doctorsWithoutClinic,
        long doctorsWithoutSpecialty,
        long doctorsWithoutCity,
        long doctorsWithoutRegion,
        long nursesWithoutClinic,
        long nursesWithoutDoctor,
        long clinicsWithoutCity,
        long citiesWithoutRegion,
        long nursesWithClinicMismatch
) {}
