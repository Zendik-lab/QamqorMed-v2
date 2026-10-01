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
        String note
) {
    public record PersonSummary(Long id, String fullName) {}
    public record StaffSummary(Long id, Long userId, String fullName, String specialty) {}
    public record ClinicSummary(Long id, String name, String cityName, String regionName) {}

    public static AppointmentResponse from(Appointment appointment) {
        var doctor = appointment.getDoctor();
        var doctorUser = doctor.getUser();
        var clinic = doctor.getClinic();
        var city = clinic.getCity();
        MedicalSubjectDto subject = appointment.getDependent() == null
                ? MedicalSubjectDto.self(appointment.getBookedBy())
                : MedicalSubjectDto.dependent(appointment.getDependent());
        return new AppointmentResponse(
                appointment.getId(), subject,
                new PersonSummary(appointment.getBookedBy().getId(), fullName(appointment.getBookedBy().getLastName(), appointment.getBookedBy().getFirstName(), appointment.getBookedBy().getMiddleName())),
                new StaffSummary(doctor.getId(), doctorUser.getId(), fullName(doctorUser.getLastName(), doctorUser.getFirstName(), doctorUser.getMiddleName()), doctor.getSpecialty().getName()),
                new ClinicSummary(clinic.getId(), clinic.getName(), city.getName(), city.getRegion().getName()),
                appointment.getAppointmentAt(), appointment.getStatus().name(), appointment.getNote());
    }

    private static String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }
}
