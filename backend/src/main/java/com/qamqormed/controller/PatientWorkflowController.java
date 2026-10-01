package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.MedicalVisitResponse;
import com.qamqormed.dto.PatientSubjectResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AppointmentService;
import com.qamqormed.service.ClinicalWorkflowService;
import com.qamqormed.service.PatientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatientWorkflowController {
    private final PatientService patients;
    private final AppointmentService appointments;
    private final ClinicalWorkflowService workflow;

    public PatientWorkflowController(PatientService patients, AppointmentService appointments, ClinicalWorkflowService workflow) {
        this.patients = patients;
        this.appointments = appointments;
        this.workflow = workflow;
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
        return workflow.patientHistory(principal.getId(), patientId);
    }

    @GetMapping("/api/appointments/{appointmentId}/result")
    public MedicalVisitResponse result(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long appointmentId) {
        return workflow.patientResult(principal.getId(), appointmentId);
    }
}
