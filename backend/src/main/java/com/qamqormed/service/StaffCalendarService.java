package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.DoctorNurseAssignment;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DoctorNurseAssignmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.NurseIntakeRepository;
import com.qamqormed.repository.NurseProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.List;

@Service
public class StaffCalendarService {
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final AppointmentRepository appointments;
    private final NurseIntakeRepository intakes;
    private final ScheduleService schedules;

    public StaffCalendarService(DoctorProfileRepository doctors, NurseProfileRepository nurses, DoctorNurseAssignmentRepository assignments,
                                AppointmentRepository appointments, NurseIntakeRepository intakes, ScheduleService schedules) {
        this.doctors = doctors; this.nurses = nurses; this.assignments = assignments; this.appointments = appointments; this.intakes = intakes; this.schedules = schedules;
    }

    @Transactional
    public StaffCalendarResponse doctorCalendar(Long userId, LocalDate from, LocalDate to) {
        return doctorCalendar(userId, from, to, null, null);
    }

    @Transactional
    public StaffCalendarResponse doctorCalendar(Long userId, LocalDate from, LocalDate to, com.qamqormed.entity.AppointmentStatus status, String query) {
        DoctorProfile doctor = doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Профиль врача не найден"));
        return calendar(doctor, from, to, status, query);
    }

    @Transactional
    public StaffCalendarResponse nurseCalendar(Long userId, LocalDate from, LocalDate to) {
        return nurseCalendar(userId, from, to, null, null);
    }

    @Transactional
    public StaffCalendarResponse nurseCalendar(Long userId, LocalDate from, LocalDate to, com.qamqormed.entity.AppointmentStatus status, String query) {
        NurseProfile nurse = nurses.findByUserId(userId).filter(NurseProfile::isActive)
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Профиль медсестры не найден"));
        DoctorNurseAssignment assignment = assignments.findActiveByNurseId(nurse.getId())
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("За медсестрой не закреплён врач"));
        return calendar(assignment.getDoctor(), from, to, status, query);
    }

    private StaffCalendarResponse calendar(DoctorProfile doctor, LocalDate requestedFrom, LocalDate requestedTo,
                                           com.qamqormed.entity.AppointmentStatus status, String query) {
        var schedule = schedules.scheduleForDoctor(doctor);
        ZoneId clinicZone = zoneOf(schedule.getTimeZone());
        LocalDate from = requestedFrom == null ? LocalDate.now(clinicZone) : requestedFrom;
        LocalDate to = requestedTo == null ? from.plusDays(6) : requestedTo;
        if (to.isBefore(from)) throw new com.qamqormed.exception.BadRequestException("Конец периода не может быть раньше начала");
        if (to.isAfter(from.plusDays(31))) to = from.plusDays(31);
        LocalDate resolvedTo = to;
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = resolvedTo.plusDays(1).atStartOfDay();
        List<CalendarEventResponse> events = appointments.findAllByDoctorIdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(doctor.getId(), start, end).stream()
                .filter(item -> matches(item, status, query))
                .map(item -> CalendarEventResponse.from(item, intakes.findByAppointmentId(item.getId()).orElse(null))).toList();
        List<ScheduleExceptionResponse> exceptions = schedule.getExceptions().stream().filter(item -> !item.getDate().isBefore(from) && !item.getDate().isAfter(resolvedTo)).map(ScheduleExceptionResponse::from).toList();
        return new StaffCalendarResponse(doctor.getId(), from, resolvedTo, DoctorScheduleResponse.from(schedule), exceptions, events);
    }

    private boolean matches(com.qamqormed.entity.Appointment appointment, com.qamqormed.entity.AppointmentStatus status, String query) {
        if (status != null && appointment.getCanonicalStatus() != status.canonical()) return false;
        if (query == null || query.isBlank()) return true;
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        String name = appointment.getDependent() != null
                ? fullName(appointment.getDependent().getLastName(), appointment.getDependent().getFirstName(), appointment.getDependent().getMiddleName())
                : appointment.getPatientProfile() != null
                ? fullName(appointment.getPatientProfile().getLastName(), appointment.getPatientProfile().getFirstName(), appointment.getPatientProfile().getMiddleName())
                : fullName(appointment.getBookedBy().getLastName(), appointment.getBookedBy().getFirstName(), appointment.getBookedBy().getMiddleName());
        return name.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }

    private ZoneId zoneOf(String value) {
        try { return value == null || value.isBlank() ? ScheduleService.CLINIC_ZONE : ZoneId.of(value); }
        catch (RuntimeException ignored) { return ScheduleService.CLINIC_ZONE; }
    }
}
