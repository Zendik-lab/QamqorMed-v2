package com.qamqormed.controller;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.AppointmentService;
import com.qamqormed.service.ClinicalWorkflowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentActionController {
    private final AppointmentService appointments; private final ClinicalWorkflowService workflow;
    public AppointmentActionController(AppointmentService appointments, ClinicalWorkflowService workflow) { this.appointments = appointments; this.workflow = workflow; }
    @PatchMapping("/{id}/confirm") public AppointmentResponse confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { return appointments.confirmForPatient(user.getId(), id); }
    @PatchMapping("/{id}/decline") public void decline(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { appointments.declineForPatient(user.getId(), id); }
    @PatchMapping("/{id}/check-in") public AppointmentResponse checkIn(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { return workflow.checkInByNurse(user.getId(), id); }
    @PatchMapping("/{id}/no-show") public AppointmentResponse noShow(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) { return workflow.noShowByNurse(user.getId(), id); }
}
