package com.qamqormed.dto;

import com.qamqormed.entity.Appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        MedicalSubjectDto subject,
        PersonSummary bookedBy,
        StaffSummary doctor,
        ClinicSummary clinic,
        LocalDateTime appointmentAt,
        String status,
        String confirmationStatus,
        java.time.Instant confirmedAt,
        java.time.Instant arrivedAt,
        String note,
        java.time.LocalTime startTime,
        java.time.LocalTime endTime
) {
    public record PersonSummary(Long id, String fullName) {}
    public record StaffSummary(Long id, Long userId, String fullName, String specialty) {}
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}

    public static AppointmentResponse from(Appointment appointment) {
        var doctor = appointment.getDoctor();
        var doctorUser = doctor.getUser();
        var clinic = doctor.getClinic();
        var city = clinic.getCity();
        MedicalSubjectDto subject = appointment.getDependent() != null
                ? MedicalSubjectDto.dependent(appointment.getDependent())
                : appointment.getPatientProfile() != null
                ? MedicalSubjectDto.self(appointment.getPatientProfile())
                : MedicalSubjectDto.self(appointment.getBookedBy());
        return new AppointmentResponse(
                appointment.getId(), subject,
                new PersonSummary(appointment.getBookedBy().getId(), fullName(appointment.getBookedBy().getLastName(), appointment.getBookedBy().getFirstName(), appointment.getBookedBy().getMiddleName())),
                new StaffSummary(doctor.getId(), doctorUser.getId(), fullName(doctorUser.getLastName(), doctorUser.getFirstName(), doctorUser.getMiddleName()), doctor.getSpecialty().getName()),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()),
                appointment.getAppointmentAt(), responseStatus(appointment),
                appointment.getConfirmationStatus() == null ? "PENDING" : appointment.getConfirmationStatus().name(),
                appointment.getConfirmedAt(), appointment.getArrivedAt(), appointment.getNote(),
                appointment.getTimeSlot() == null ? appointment.getAppointmentAt().toLocalTime() : appointment.getTimeSlot().getStartsAt().toLocalTime(),
                appointment.getTimeSlot() == null ? appointment.getAppointmentAt().toLocalTime().plusMinutes(30) : appointment.getTimeSlot().getEndsAt().toLocalTime());
    }

    /**
     * New appointments use the Stage 7 lifecycle names. Legacy rows created
     * before patient_profiles existed remain readable with their old labels.
     */
    private static String responseStatus(Appointment appointment) {
        boolean stage7Subject = appointment.getPatientProfile() != null
                || (appointment.getDependent() != null && appointment.getDependent().getPatientProfile() != null);
        if (stage7Subject) return appointment.getCanonicalStatus().name();
        return switch (appointment.getCanonicalStatus()) {
            case PREPARING -> "NURSE_IN_PROGRESS";
            case READY -> "READY_FOR_DOCTOR";
            case IN_APPOINTMENT -> "DOCTOR_IN_PROGRESS";
            default -> appointment.getCanonicalStatus().name();
        };
    }

    private static String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }
}
