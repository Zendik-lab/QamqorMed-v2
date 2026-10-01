package com.qamqormed.controller;

import com.qamqormed.dto.*;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ClinicalStructuredService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class StructuredClinicalController {
    private final ClinicalStructuredService service;
    public StructuredClinicalController(ClinicalStructuredService service) { this.service = service; }

    @PostMapping("/api/doctor/appointments/{appointmentId}/diagnoses")
    public VisitDiagnosisResponse diagnosis(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @Valid @RequestBody VisitDiagnosisRequest request) {
        return service.addDiagnosis(user.getId(), appointmentId, request);
    }
    @GetMapping("/api/doctor/appointments/{appointmentId}/diagnoses")
    public List<VisitDiagnosisResponse> diagnoses(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId) {
        return service.diagnosesForDoctor(user.getId(), appointmentId);
    }
    @PostMapping("/api/doctor/appointments/{appointmentId}/prescription-orders")
    public PrescriptionResponse prescription(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @RequestBody PrescriptionCreateRequest request) {
        return service.addPrescription(user.getId(), appointmentId, request);
    }
    @PostMapping("/api/doctor/appointments/{appointmentId}/medications")
    public MedicationPrescriptionResponse medication(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @RequestBody MedicationPrescriptionRequest request) {
        return service.addMedication(user.getId(), appointmentId, request);
    }
    @PostMapping("/api/doctor/appointments/{appointmentId}/lab-results")
    public LabResultResponse labResult(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @Valid @RequestBody LabResultCreateRequest request) {
        return service.addLabResult(user.getId(), appointmentId, request);
    }
    @PostMapping("/api/doctor/appointments/{appointmentId}/certificates")
    public MedicalCertificateResponse certificate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long appointmentId, @Valid @RequestBody MedicalCertificateCreateRequest request) {
        return service.addCertificate(user.getId(), appointmentId, request);
    }
    @GetMapping("/api/patient/diagnoses")
    public List<VisitDiagnosisResponse> patientDiagnoses(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long subjectId) { return service.patientDiagnoses(user.getId(), subjectId); }
    @GetMapping("/api/patient/prescriptions")
    public List<PrescriptionResponse> patientPrescriptions(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long subjectId) { return service.patientPrescriptions(user.getId(), subjectId); }
    @GetMapping("/api/patient/medications")
    public List<MedicationPrescriptionResponse> patientMedications(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long subjectId) { return service.patientMedications(user.getId(), subjectId); }
    @GetMapping("/api/patient/lab-results")
    public List<LabResultResponse> patientLabResults(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long subjectId) { return service.patientLabResults(user.getId(), subjectId); }
    @GetMapping("/api/patient/certificates")
    public List<MedicalCertificateResponse> patientCertificates(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long subjectId) { return service.patientCertificates(user.getId(), subjectId); }
}
