package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.MedicalRecordCreateRequest;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ClinicalWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/appointments")
public class DoctorWorkflowController {
    private final ClinicalWorkflowService workflow;

    public DoctorWorkflowController(ClinicalWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public List<AppointmentResponse> appointments(@AuthenticationPrincipal AuthenticatedUser principal) {
        return workflow.doctorAppointments(principal.getId());
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse appointment(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.doctorAppointment(principal.getId(), appointmentId);
    }

    @GetMapping("/{appointmentId}/medical-records")
    public List<MedicalRecordResponse> medicalHistory(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.doctorSubjectHistory(principal.getId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/medical-record")
    public MedicalRecordResponse medicalRecord(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long appointmentId,
                                                @RequestBody MedicalRecordCreateRequest request) {
        return workflow.saveMedicalRecord(principal.getId(), appointmentId, request);
    }

    @PatchMapping("/{appointmentId}/status")
    public AppointmentResponse status(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long appointmentId,
                                      @Valid @RequestBody AppointmentStatusRequest request) {
        return workflow.updateDoctorStatus(principal.getId(), appointmentId, request);
    }
}
