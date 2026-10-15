package com.qamqormed.controller;

import com.qamqormed.dto.DevNotificationWorkflowResponse;
import com.qamqormed.dto.PageResponse;
import com.qamqormed.service.DevNotificationWorkflowService;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Read-only appointment inventory for the dev workflow panel. */
@RestController
@Profile("dev")
@RequestMapping("/api/dev/appointments")
public class DevAppointmentController {
    private final DevNotificationWorkflowService service;

    public DevAppointmentController(DevNotificationWorkflowService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<DevNotificationWorkflowResponse.AppointmentView> search(
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String doctor,
            @RequestParam(required = false) String clinic,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String confirmationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return PageResponse.from(service.searchAll(appointmentId, patient, doctor, clinic, regionId, cityId,
                clinicId, doctorId, appointmentDate, status, confirmationStatus, page, size, sort), value -> value);
    }
}
