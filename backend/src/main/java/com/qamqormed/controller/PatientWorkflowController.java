package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.MedicalVisitResponse;
import com.qamqormed.dto.PatientSubjectResponse;
import com.qamqormed.dto.MedicalHistoryResponse;
import com.qamqormed.dto.MedicalHistorySummaryResponse;
import com.qamqormed.dto.MedicalDocumentResponse;
import com.qamqormed.dto.MedicalCertificateResponse;
import com.qamqormed.dto.LabResultResponse;
import com.qamqormed.dto.MedicationPrescriptionResponse;
import com.qamqormed.dto.PrescriptionResponse;
import com.qamqormed.dto.ReferralResponse;
import com.qamqormed.dto.VisitDiagnosisResponse;
import com.qamqormed.dto.AttendanceResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AppointmentService;
import com.qamqormed.service.ClinicalWorkflowService;
import com.qamqormed.service.PatientService;
import com.qamqormed.service.MedicalHistoryService;
import com.qamqormed.service.AttendanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatientWorkflowController {
    private final PatientService patients;
    private final AppointmentService appointments;
    private final ClinicalWorkflowService workflow;
    private final MedicalHistoryService history;
    private final AttendanceService attendance;

    public PatientWorkflowController(PatientService patients, AppointmentService appointments,
                                      ClinicalWorkflowService workflow, MedicalHistoryService history, AttendanceService attendance) {
        this.patients = patients;
        this.appointments = appointments;
        this.workflow = workflow;
        this.history = history;
        this.attendance = attendance;
    }

    @GetMapping("/api/patients/{patientId}/attendance")
    public AttendanceResponse attendance(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long patientId) {
        return attendance.forSubject(principal.getId(), patientId);
    }

    @GetMapping("/api/patient-profiles")
    public List<PatientSubjectResponse> profiles(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.subjects(principal.getId());
    }

    @GetMapping("/api/patients/{patientId}/appointments")
    public List<AppointmentResponse> subjectAppointments(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long patientId) {
        return appointments.patientAppointmentsForSubject(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/medical-history")
    public List<MedicalVisitResponse> subjectHistory(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @PathVariable Long patientId) {
        return history.visitResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patient/medical-history")
    public MedicalHistoryResponse medicalHistory(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @RequestParam(required = false) Long subjectId) {
        return history.history(principal.getId(), subjectId);
    }

    @GetMapping("/api/patients/{patientId}/medical-history/overview")
    public MedicalHistoryResponse medicalHistoryOverview(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long patientId) {
        return history.history(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/medical-history/summary")
    public MedicalHistorySummaryResponse medicalHistorySummary(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @PathVariable Long patientId) {
        return history.summary(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/visits")
    public List<MedicalVisitResponse> visits(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable Long patientId) {
        return history.visitResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/visits/{visitId}")
    public MedicalVisitResponse visit(@AuthenticationPrincipal AuthenticatedUser principal,
                                      @PathVariable Long patientId, @PathVariable Long visitId) {
        return history.visit(principal.getId(), patientId, visitId);
    }

    @GetMapping("/api/patients/{patientId}/diagnoses")
    public List<VisitDiagnosisResponse> diagnoses(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable Long patientId) {
        return history.diagnosisResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/prescriptions")
    public List<PrescriptionResponse> prescriptions(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @PathVariable Long patientId) {
        return history.prescriptionResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/medications")
    public List<MedicationPrescriptionResponse> medications(@AuthenticationPrincipal AuthenticatedUser principal,
                                                             @PathVariable Long patientId) {
        return history.medicationResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/lab-results")
    public List<LabResultResponse> labResults(@AuthenticationPrincipal AuthenticatedUser principal,
                                              @PathVariable Long patientId) {
        return history.labResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/referrals")
    public List<ReferralResponse> referrals(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable Long patientId) {
        return history.referralResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/certificates")
    public List<MedicalCertificateResponse> certificates(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long patientId) {
        return history.certificateResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patient/referrals")
    public List<ReferralResponse> patientReferrals(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @RequestParam(required = false) Long subjectId) {
        return history.referralResponses(principal.getId(), subjectId);
    }

    @GetMapping("/api/patient/documents")
    public List<MedicalDocumentResponse> patientDocuments(@AuthenticationPrincipal AuthenticatedUser principal,
                                                          @RequestParam(required = false) Long subjectId) {
        return history.documentResponses(principal.getId(), subjectId);
    }

    @GetMapping("/api/patients/{patientId}/documents")
    public List<MedicalDocumentResponse> documents(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable Long patientId) {
        return history.documentResponses(principal.getId(), patientId);
    }

    @GetMapping("/api/patients/{patientId}/documents/{documentId}")
    public MedicalDocumentResponse document(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable Long patientId, @PathVariable Long documentId) {
        return history.document(principal.getId(), patientId, documentId);
    }

    @GetMapping("/api/patient/documents/{documentId}")
    public MedicalDocumentResponse ownDocument(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long documentId,
                                               @RequestParam(required = false) Long subjectId) {
        return history.document(principal.getId(), subjectId, documentId);
    }

    @GetMapping("/api/patients/{patientId}/visits/{visitId}/documents")
    public List<MedicalDocumentResponse> visitDocuments(@AuthenticationPrincipal AuthenticatedUser principal,
                                                         @PathVariable Long patientId, @PathVariable Long visitId) {
        return history.visitDocuments(principal.getId(), patientId, visitId);
    }

    @GetMapping("/api/appointments/{appointmentId}/result")
    public MedicalVisitResponse result(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long appointmentId) {
        return workflow.patientResult(principal.getId(), appointmentId);
    }
}
