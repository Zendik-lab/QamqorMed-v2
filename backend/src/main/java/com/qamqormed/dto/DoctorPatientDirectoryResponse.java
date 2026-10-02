package com.qamqormed.dto;

import java.util.List;

public record DoctorPatientDirectoryResponse(
        List<AppointmentDetailsResponse> today,
        List<AppointmentDetailsResponse> upcoming,
        List<AppointmentDetailsResponse> history,
        List<DoctorPatientSummaryResponse> patients
) {}
