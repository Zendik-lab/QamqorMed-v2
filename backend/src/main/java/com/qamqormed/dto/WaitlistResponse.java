package com.qamqormed.dto;
import com.qamqormed.entity.WaitlistEntry;
import java.time.*;
public record WaitlistResponse(Long id, Long dependentId, String patientName, Long clinicId, String clinicName,
                               Long specialtyId, String specialtyName, Long doctorId, String doctorName,
                               LocalDate desiredDateFrom, LocalDate desiredDateTo, LocalTime preferredTimeFrom,
                               LocalTime preferredTimeTo, String status, Instant createdAt) {
    public static WaitlistResponse from(WaitlistEntry value) {
        var user = value.getPatientUser();
        String patient = value.getDependent() == null ? fullName(user.getLastName(), user.getFirstName(), user.getMiddleName())
                : fullName(value.getDependent().getLastName(), value.getDependent().getFirstName(), value.getDependent().getMiddleName());
        String doctor = value.getDoctor() == null ? null : fullName(value.getDoctor().getUser().getLastName(), value.getDoctor().getUser().getFirstName(), value.getDoctor().getUser().getMiddleName());
        return new WaitlistResponse(value.getId(), value.getDependent() == null ? null : value.getDependent().getId(), patient,
                value.getClinic().getId(), value.getClinic().getName(), value.getSpecialty().getId(), value.getSpecialty().getName(),
                value.getDoctor() == null ? null : value.getDoctor().getId(), doctor, value.getDesiredDateFrom(), value.getDesiredDateTo(),
                value.getPreferredTimeFrom(), value.getPreferredTimeTo(), value.getStatus().name(), value.getCreatedAt());
    }
    private static String fullName(String last, String first, String middle) { return String.join(" ", last, first, middle == null ? "" : middle).trim().replaceAll(" +", " "); }
}
