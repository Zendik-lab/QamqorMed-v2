package com.qamqormed.controller;

import com.qamqormed.dto.DevNotificationWorkflowResponse;
import com.qamqormed.service.DevNotificationWorkflowService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/notification-workflow")
public class DevNotificationWorkflowController {
    private final DevNotificationWorkflowService service;

    public DevNotificationWorkflowController(DevNotificationWorkflowService service) { this.service = service; }

    @GetMapping("/appointments")
    public List<DevNotificationWorkflowResponse.AppointmentView> search(
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String doctor,
            @RequestParam(required = false) String clinic) {
        return service.search(appointmentId, patient, doctor, clinic);
    }

    @GetMapping("/appointments/{appointmentId}")
    public DevNotificationWorkflowResponse inspect(@PathVariable Long appointmentId) { return service.inspect(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/reminder-24h")
    public DevNotificationWorkflowResponse reminder24h(@PathVariable Long appointmentId) { return service.reminder24h(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/reminder-2h")
    public DevNotificationWorkflowResponse reminder2h(@PathVariable Long appointmentId) { return service.reminder2h(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/confirmation-request")
    public DevNotificationWorkflowResponse confirmationRequest(@PathVariable Long appointmentId) { return service.confirmationRequest(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/no-show")
    public DevNotificationWorkflowResponse noShow(@PathVariable Long appointmentId) { return service.simulateNoShow(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/arrived")
    public DevNotificationWorkflowResponse arrived(@PathVariable Long appointmentId) { return service.markArrived(appointmentId); }

    @PostMapping("/appointments/{appointmentId}/waitlist-check")
    public DevNotificationWorkflowResponse waitlistCheck(@PathVariable Long appointmentId) { return service.checkWaitlist(appointmentId); }
}
