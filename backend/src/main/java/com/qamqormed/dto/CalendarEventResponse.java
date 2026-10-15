package com.qamqormed.dto;

import com.qamqormed.entity.Appointment;
import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.PatientProfile;
import com.qamqormed.entity.NurseIntake;
import com.qamqormed.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.LocalTime;
import java.math.BigDecimal;

public record CalendarEventResponse(Long id, LocalDate date, LocalTime startTime, LocalTime endTime,
                                    Recipient recipient, String status, String note, boolean nursePrepared, NurseAssessmentSummary nurseAssessment,
                                    String specialty) {
    public record Recipient(Long id, String fullName, Integer age, String gender, String type, String relationship) {}
    public record NurseAssessmentSummary(BigDecimal temperature, Integer bloodPressureSystolic, Integer bloodPressureDiastolic,
                                         Integer pulse, Integer oxygenSaturation, BigDecimal weight, BigDecimal height,
                                         String complaintSummary, String nurseNotes) {}

    public static CalendarEventResponse from(Appointment appointment, NurseIntake intake) {
        LocalDateTime start = appointment.getAppointmentAt();
        LocalTime end = appointment.getTimeSlot() == null ? start.toLocalTime().plusMinutes(30) : appointment.getTimeSlot().getEndsAt().toLocalTime();
        if (end.equals(start.toLocalTime())) end = end.plusMinutes(30);
        if (appointment.getDependent() != null) {
            Dependent value = appointment.getDependent();
            return new CalendarEventResponse(appointment.getId(), start.toLocalDate(), start.toLocalTime(), end,
                    new Recipient(value.getId(), fullName(value.getLastName(), value.getFirstName(), value.getMiddleName()), age(value.getDateOfBirth(), start.toLocalDate()), value.getGender(), "DEPENDENT", value.getRelationship() == null ? null : value.getRelationship().name()),
                    appointment.getCanonicalStatus().name(), appointment.getNote(), intake != null, assessment(intake), specialty(appointment));
        }
        PatientProfile profile = appointment.getPatientProfile();
        User user = profile == null ? appointment.getBookedBy() : profile.getUser();
        LocalDate birthDate = profile == null ? user.getDateOfBirth() : profile.getBirthDate();
        String gender = profile == null ? user.getGender() : profile.getGender() == null ? null : profile.getGender().name();
        return new CalendarEventResponse(appointment.getId(), start.toLocalDate(), start.toLocalTime(), end,
                new Recipient(profile == null ? user.getId() : profile.getId(), fullName(profile == null ? user.getLastName() : profile.getLastName(), profile == null ? user.getFirstName() : profile.getFirstName(), profile == null ? user.getMiddleName() : profile.getMiddleName()), age(birthDate, start.toLocalDate()), gender, "PATIENT", null),
                appointment.getCanonicalStatus().name(), appointment.getNote(), intake != null, assessment(intake), specialty(appointment));
    }

    private static NurseAssessmentSummary assessment(NurseIntake intake) {
        return intake == null ? null : new NurseAssessmentSummary(intake.getTemperature(), intake.getBloodPressureSystolic(), intake.getBloodPressureDiastolic(), intake.getPulse(), intake.getOxygenSaturation(), intake.getWeight(), intake.getHeight(), intake.getComplaintSummary(), intake.getNurseNotes());
    }

    private static Integer age(LocalDate birthDate, LocalDate at) { return birthDate == null ? null : Period.between(birthDate, at).getYears(); }
    private static String specialty(Appointment appointment) { return appointment.getDoctor().getSpecialty() == null ? null : appointment.getDoctor().getSpecialty().getName(); }
    private static String fullName(String lastName, String firstName, String middleName) { return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " "); }
}
