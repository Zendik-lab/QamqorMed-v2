package com.qamqormed.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Read model returned by the dev-only notification/attendance/waitlist test panel. */
public record DevNotificationWorkflowResponse(
        AppointmentView appointment,
        List<StatusHistoryView> history,
        List<NotificationView> notifications,
        List<WaitlistView> waitlist,
        SchedulerView scheduler,
        ActionResult lastAction
) {
    public record AppointmentView(Long id, Long bookedByUserId, String bookedByName,
                                  String patientName, String dependentName, Long dependentId,
                                  Long doctorProfileId, Long doctorUserId, String doctorName,
                                  String specialtyName, Long clinicId, String clinicName,
                                  String cityName, String regionName, LocalDateTime appointmentAt,
                                  String status, String canonicalStatus, String confirmationStatus,
                                  Instant createdAt, Instant confirmedAt, Instant arrivedAt,
                                  Instant noShowAt, Long timeSlotId, boolean slotReleased,
                                  boolean demo) { }

    public record StatusHistoryView(Long id, String oldStatus, String newStatus, Instant changedAt,
                                    Long changedByUserId, String changedByName, String reason) { }

    public record NotificationView(Long id, String type, String title, String message, Instant createdAt,
                                   Instant readAt, boolean read, String eventKey) { }

    public record WaitlistView(Long id, Long patientUserId, String patientName, Long dependentId,
                               Long doctorProfileId, String status, LocalDate desiredDateFrom,
                               LocalDate desiredDateTo, LocalTime preferredTimeFrom, LocalTime preferredTimeTo) { }

    public record SchedulerView(Instant evaluatedAt, List<Long> reminder24hAppointmentIds,
                                List<Long> reminder2hAppointmentIds, List<Long> noShowAppointmentIds) { }

    public record ActionResult(String action, String status, String message, List<String> details,
                               Instant executedAt) { }
}
