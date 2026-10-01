package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.NurseIntakeRequest;
import com.qamqormed.dto.NurseIntakeResponse;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.dto.AppointmentDetailsResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ClinicalWorkflowService;
import com.qamqormed.entity.AppointmentStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nurse/appointments")
public class NurseWorkflowController {
    private final ClinicalWorkflowService workflow;

    public NurseWorkflowController(ClinicalWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public List<AppointmentResponse> appointments(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @RequestParam(required = false) java.time.LocalDate date,
                                                  @RequestParam(required = false) AppointmentStatus status,
                                                  @RequestParam(required = false) String query) {
        return workflow.nurseAppointments(principal.getId(), date, status, query);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse appointment(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.nurseAppointment(principal.getId(), appointmentId);
    }

    @GetMapping("/{appointmentId}/details")
    public AppointmentDetailsResponse details(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.nurseAppointmentDetails(principal.getId(), appointmentId);
    }

    @GetMapping("/{appointmentId}/medical-records")
    public List<MedicalRecordResponse> medicalHistory(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.nurseSubjectHistory(principal.getId(), appointmentId);
    }

    @PostMapping("/{appointmentId}/intake")
    public NurseIntakeResponse intake(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long appointmentId,
                                      @Valid @RequestBody NurseIntakeRequest request) {
        return workflow.saveNurseIntake(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/start")
    public AppointmentDetailsResponse start(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.startNurseAssessment(principal.getId(), appointmentId);
    }

    @PutMapping("/{appointmentId}/assessment")
    public NurseIntakeResponse assessment(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @PathVariable Long appointmentId,
                                           @Valid @RequestBody NurseIntakeRequest request) {
        return workflow.saveNurseIntake(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/ready-for-doctor")
    public AppointmentDetailsResponse readyForDoctor(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.readyForDoctor(principal.getId(), appointmentId);
    }

    @PatchMapping("/{appointmentId}/status")
    public AppointmentResponse status(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId,
                                      @Valid @RequestBody AppointmentStatusRequest request) {
        return workflow.updateNurseStatus(principal.getId(), appointmentId, request);
    }
}
