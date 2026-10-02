package com.qamqormed.dto;

import java.util.List;

public record DoctorPatientDetailResponse(
        DoctorPatientSummaryResponse patient,
        AppointmentDetailsResponse currentAppointment,
        List<AppointmentDetailsResponse> appointments,
        List<MedicalVisitResponse> visits,
        List<LabResultResponse> labResults,
        List<MedicalCertificateResponse> certificates
) {}
