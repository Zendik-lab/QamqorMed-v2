package com.qamqormed.controller;

import com.qamqormed.dto.DoctorScheduleResponse;
import com.qamqormed.dto.DoctorScheduleUpdateRequest;
import com.qamqormed.dto.StaffCalendarResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ScheduleService;
import com.qamqormed.service.StaffCalendarService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class StaffCalendarController {
    private final StaffCalendarService calendar;
    private final ScheduleService schedules;
    public StaffCalendarController(StaffCalendarService calendar, ScheduleService schedules) { this.calendar = calendar; this.schedules = schedules; }

    @GetMapping({"/api/doctor/calendar", "/api/doctor/schedule", "/api/doctors/me/calendar", "/api/doctors/me/schedule"})
    public StaffCalendarResponse doctor(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) com.qamqormed.entity.AppointmentStatus status,
                                        @RequestParam(required = false) String query) {
        return calendar.doctorCalendar(principal.getId(), from, to, status, query);
    }

    @GetMapping({"/api/nurse/calendar", "/api/nurse/schedule", "/api/nurses/me/calendar", "/api/nurses/me/schedule"})
    public StaffCalendarResponse nurse(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) com.qamqormed.entity.AppointmentStatus status,
                                       @RequestParam(required = false) String query) {
        return calendar.nurseCalendar(principal.getId(), from, to, status, query);
    }

    @GetMapping("/api/admin/doctors/{doctorId}/schedule")
    public DoctorScheduleResponse adminSchedule(@PathVariable Long doctorId) { return schedules.response(doctorId); }

    @PutMapping("/api/admin/doctors/{doctorId}/schedule")
    public DoctorScheduleResponse updateSchedule(@PathVariable Long doctorId, @Valid @RequestBody DoctorScheduleUpdateRequest request) {
        return schedules.update(doctorId, request);
    }
}
