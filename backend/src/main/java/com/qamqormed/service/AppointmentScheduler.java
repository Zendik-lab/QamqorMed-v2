package com.qamqormed.service;

import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class AppointmentScheduler {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");
    private final AppointmentRepository appointments; private final NotificationService notifications; private final AppointmentStatusHistoryService history;
    private final Clock clock;

    @Autowired
    public AppointmentScheduler(AppointmentRepository appointments, NotificationService notifications,
                                 AppointmentStatusHistoryService history, Clock clock) {
        this.appointments = appointments; this.notifications = notifications; this.history = history; this.clock = clock;
    }

    /** Kept for focused unit tests and callers that construct the scheduler directly. */
    public AppointmentScheduler(AppointmentRepository appointments, NotificationService notifications,
                                 AppointmentStatusHistoryService history) {
        this(appointments, notifications, history, Clock.system(CLINIC_ZONE));
    }

    @Scheduled(fixedDelayString = "${app.scheduler.interval-ms:60000}")
    @Transactional
    public void process() {
        processAt(clock.instant());
    }

    /** Runs the same production scheduler logic at a supplied instant. */
    @Transactional
    public SchedulerRunResult processAt(Instant evaluatedAt) {
        LocalDateTime now = LocalDateTime.ofInstant(evaluatedAt, CLINIC_ZONE);
        List<Long> reminder24h = processReminders(now.plusHours(23), now.plusHours(25),
                NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED, "reminder-24-");
        List<Long> reminder2h = processReminders(now.plusHours(1), now.plusHours(3),
                NotificationType.APPOINTMENT_REMINDER, "reminder-2-");
        List<Long> noShows = new ArrayList<>();
        appointments.findAllByAppointmentAtBeforeAndStatusIn(now.minusMinutes(30), List.of(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED)).forEach(item -> {
            AppointmentStatus old = item.getCanonicalStatus(); item.setStatus(AppointmentStatus.NO_SHOW); item.setNoShowAt(evaluatedAt); appointments.save(item); history.record(item, old, AppointmentStatus.NO_SHOW, null, "grace-period");
            noShows.add(item.getId());
        });
        return new SchedulerRunResult(evaluatedAt, reminder24h, reminder2h, noShows);
    }

    private List<Long> processReminders(LocalDateTime from, LocalDateTime to, NotificationType type, String keyPrefix) {
        List<Long> processed = new ArrayList<>();
        appointments.findAllByAppointmentAtBetweenAndStatusIn(from, to, List.of(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED)).forEach(item -> {
            if (type == NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED && item.getConfirmationStatus() == AppointmentConfirmationStatus.CONFIRMED) return;
            notifications.reminder(item, type, keyPrefix + item.getId());
            processed.add(item.getId());
        });
        return processed;
    }

    public record SchedulerRunResult(Instant evaluatedAt, List<Long> reminder24hAppointmentIds,
                                     List<Long> reminder2hAppointmentIds, List<Long> noShowAppointmentIds) { }
}
