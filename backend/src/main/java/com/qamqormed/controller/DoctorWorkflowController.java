package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.MedicalRecordCreateRequest;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.dto.AppointmentDetailsResponse;
import com.qamqormed.dto.MedicalDocumentRequest;
import com.qamqormed.dto.MedicalDocumentResponse;
import com.qamqormed.dto.MedicalVisitResponse;
import com.qamqormed.dto.MedicalVisitUpsertRequest;
import com.qamqormed.dto.ReferralRequest;
import com.qamqormed.dto.ReferralResponse;
import com.qamqormed.dto.TreatmentRequest;
import com.qamqormed.dto.TreatmentResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/appointments")
public class DoctorWorkflowController {
    private final ClinicalWorkflowService workflow;

    public DoctorWorkflowController(ClinicalWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public List<AppointmentResponse> appointments(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @RequestParam(required = false) java.time.LocalDate date,
                                                  @RequestParam(required = false) com.qamqormed.entity.AppointmentStatus status,
                                                  @RequestParam(required = false) String query) {
        return workflow.doctorAppointments(principal.getId(), date, status, query);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse appointment(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.doctorAppointment(principal.getId(), appointmentId);
    }

    @GetMapping("/{appointmentId}/details")
    public AppointmentDetailsResponse details(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.doctorAppointmentDetails(principal.getId(), appointmentId);
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

    @PostMapping("/{appointmentId}/start")
    public AppointmentDetailsResponse start(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.startDoctorVisit(principal.getId(), appointmentId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{appointmentId}/visit")
    public MedicalVisitResponse visit(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long appointmentId,
                                      @RequestBody MedicalVisitUpsertRequest request) {
        return workflow.saveMedicalVisit(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/prescriptions")
    public TreatmentResponse prescription(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @PathVariable Long appointmentId,
                                           @RequestBody TreatmentRequest request) {
        return workflow.addTreatment(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/referrals")
    public ReferralResponse referral(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @PathVariable Long appointmentId,
                                     @RequestBody ReferralRequest request) {
        return workflow.addReferral(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/documents")
    public MedicalDocumentResponse document(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable Long appointmentId,
                                            @RequestBody MedicalDocumentRequest request) {
        return workflow.addDocument(principal.getId(), appointmentId, request);
    }

    @PostMapping("/{appointmentId}/complete")
    public AppointmentDetailsResponse complete(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        return workflow.completeDoctorVisit(principal.getId(), appointmentId);
    }

    @PatchMapping("/{appointmentId}/status")
    public AppointmentResponse status(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long appointmentId,
                                      @Valid @RequestBody AppointmentStatusRequest request) {
        return workflow.updateDoctorStatus(principal.getId(), appointmentId, request);
    }
}
