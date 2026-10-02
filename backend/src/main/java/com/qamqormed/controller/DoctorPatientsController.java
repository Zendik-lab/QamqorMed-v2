package com.qamqormed.controller;

import com.qamqormed.dto.DoctorPatientDetailResponse;
import com.qamqormed.dto.DoctorPatientDirectoryResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.ClinicalWorkflowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/doctor/patients")
public class DoctorPatientsController {
    private final ClinicalWorkflowService workflow;

    public DoctorPatientsController(ClinicalWorkflowService workflow) { this.workflow = workflow; }

    @GetMapping
    public DoctorPatientDirectoryResponse directory(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long clinicId) {
        return workflow.doctorPatientDirectory(principal.getId(), query, status, date, period, type, clinicId);
    }

    @GetMapping("/{patientId}")
    public DoctorPatientDetailResponse detail(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long patientId,
            @RequestParam(required = false) String type) {
        return workflow.doctorPatientDetail(principal.getId(), patientId, type);
    }
}
