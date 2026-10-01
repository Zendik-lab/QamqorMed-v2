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
        DoctorProfile doctor = doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Профиль врача не найден"));
        return calendar(doctor, from, to);
    }

    @Transactional
    public StaffCalendarResponse nurseCalendar(Long userId, LocalDate from, LocalDate to) {
        NurseProfile nurse = nurses.findByUserId(userId).filter(NurseProfile::isActive)
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Профиль медсестры не найден"));
        DoctorNurseAssignment assignment = assignments.findActiveByNurseId(nurse.getId())
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("За медсестрой не закреплён врач"));
        return calendar(assignment.getDoctor(), from, to);
    }

    private StaffCalendarResponse calendar(DoctorProfile doctor, LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate from = requestedFrom == null ? LocalDate.now(ScheduleService.CLINIC_ZONE) : requestedFrom;
        LocalDate to = requestedTo == null ? from.plusDays(6) : requestedTo;
        if (to.isBefore(from)) throw new com.qamqormed.exception.BadRequestException("Конец периода не может быть раньше начала");
        if (to.isAfter(from.plusDays(31))) to = from.plusDays(31);
        LocalDate resolvedTo = to;
        var schedule = schedules.scheduleForDoctor(doctor);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = resolvedTo.plusDays(1).atStartOfDay();
        List<CalendarEventResponse> events = appointments.findAllByDoctorIdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(doctor.getId(), start, end).stream()
                .filter(item -> item.getCanonicalStatus() != com.qamqormed.entity.AppointmentStatus.CANCELLED)
                .map(item -> CalendarEventResponse.from(item, intakes.findByAppointmentId(item.getId()).orElse(null))).toList();
        List<ScheduleExceptionResponse> exceptions = schedule.getExceptions().stream().filter(item -> !item.getDate().isBefore(from) && !item.getDate().isAfter(resolvedTo)).map(ScheduleExceptionResponse::from).toList();
        return new StaffCalendarResponse(doctor.getId(), from, resolvedTo, DoctorScheduleResponse.from(schedule), exceptions, events);
    }
}
