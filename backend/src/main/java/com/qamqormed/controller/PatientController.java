package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentCreateRequest;
import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.DependentRequest;
import com.qamqormed.dto.DependentResponse;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.dto.PatientProfileDto;
import com.qamqormed.dto.PatientProfileUpdateRequest;
import com.qamqormed.dto.PatientLocationDto;
import com.qamqormed.dto.PatientLocationUpdateRequest;
import com.qamqormed.dto.AppointmentCancellationRequest;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AppointmentService;
import com.qamqormed.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
public class PatientController {
    private final PatientService patients;
    private final AppointmentService appointments;

    public PatientController(PatientService patients, AppointmentService appointments) {
        this.patients = patients;
        this.appointments = appointments;
    }

    @GetMapping("/profile")
    public PatientProfileDto profile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.profile(principal.getId());
    }

    @PutMapping("/profile")
    public PatientProfileDto updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @RequestBody PatientProfileUpdateRequest request) {
        return patients.updateProfile(principal.getId(), request);
    }

    @GetMapping("/location")
    public PatientLocationDto location(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.location(principal.getId());
    }

    @PutMapping("/location")
    public PatientLocationDto updateLocation(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @Valid @RequestBody PatientLocationUpdateRequest request) {
        return patients.updateLocation(principal.getId(), request);
    }

    @GetMapping("/dependents")
    public List<DependentResponse> dependents(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.listDependents(principal.getId());
    }

    @GetMapping("/dependents/{dependentId}")
    public DependentResponse dependent(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long dependentId) {
        return patients.getDependent(principal.getId(), dependentId);
    }

    @PostMapping("/dependents")
    public ResponseEntity<DependentResponse> createDependent(@AuthenticationPrincipal AuthenticatedUser principal,
                                                              @Valid @RequestBody DependentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patients.createDependent(principal.getId(), request));
    }

    @PutMapping("/dependents/{dependentId}")
    public DependentResponse updateDependent(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable Long dependentId, @Valid @RequestBody DependentRequest request) {
        return patients.updateDependent(principal.getId(), dependentId, request);
    }

    @DeleteMapping("/dependents/{dependentId}")
    public ResponseEntity<Void> deleteDependent(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long dependentId) {
        patients.deactivateDependent(principal.getId(), dependentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments(@AuthenticationPrincipal AuthenticatedUser principal) {
        return appointments.patientAppointments(principal.getId());
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                  @Valid @RequestBody AppointmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.createForPatient(principal.getId(), request));
    }

    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId) {
        appointments.cancelForPatient(principal.getId(), appointmentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<Void> cancelAppointment(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long appointmentId,
                                                   @RequestBody(required = false) AppointmentCancellationRequest request) {
        appointments.cancelForPatient(principal.getId(), appointmentId, request == null ? null : request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/medical-records")
    public List<MedicalRecordResponse> ownMedicalRecords(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.ownMedicalRecords(principal.getId());
    }

    @GetMapping("/dependents/{dependentId}/medical-records")
    public List<MedicalRecordResponse> dependentMedicalRecords(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                @PathVariable Long dependentId) {
        return patients.dependentMedicalRecords(principal.getId(), dependentId);
    }
}
