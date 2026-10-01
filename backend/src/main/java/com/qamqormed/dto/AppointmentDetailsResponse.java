package com.qamqormed.dto;

import com.qamqormed.entity.Appointment;

public record AppointmentDetailsResponse(
        Long id,
        MedicalSubjectDto subject,
        AppointmentResponse.PersonSummary bookedBy,
        AppointmentResponse.StaffSummary doctor,
        AppointmentResponse.ClinicSummary clinic,
        java.time.LocalDateTime appointmentAt,
        String status,
        String note,
        NurseIntakeResponse nurseAssessment,
        MedicalVisitResponse medicalVisit,
        boolean readOnly
) {
    public static AppointmentDetailsResponse from(Appointment appointment, NurseIntakeResponse assessment,
                                                   MedicalVisitResponse visit) {
        AppointmentResponse summary = AppointmentResponse.from(appointment);
        return new AppointmentDetailsResponse(summary.id(), summary.subject(), summary.bookedBy(), summary.doctor(),
                summary.clinic(), summary.appointmentAt(), summary.status(), summary.note(), assessment, visit,
                appointment.getCanonicalStatus() == com.qamqormed.entity.AppointmentStatus.COMPLETED);
    }
}
