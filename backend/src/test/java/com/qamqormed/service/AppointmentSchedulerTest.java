package com.qamqormed.service;

import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AppointmentSchedulerTest {
    @Test
    void movesOnlyExpiredUnattendedAppointmentsToNoShow() {
        AppointmentRepository repository = mock(AppointmentRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        AppointmentStatusHistoryService history = mock(AppointmentStatusHistoryService.class);
        AppointmentScheduler scheduler = new AppointmentScheduler(repository, notifications, history);
        Appointment expired = new Appointment(); expired.setAppointmentAt(LocalDateTime.now().minusMinutes(31)); expired.setStatus(AppointmentStatus.BOOKED);
        when(repository.findAllByAppointmentAtBeforeAndStatusIn(any(), any())).thenReturn(List.of(expired));
        when(repository.findAllByAppointmentAtBetweenAndStatusIn(any(), any(), any())).thenReturn(List.of());
        scheduler.process();
        assertEquals(AppointmentStatus.NO_SHOW, expired.getStatus());
        verify(history).record(expired, AppointmentStatus.BOOKED, AppointmentStatus.NO_SHOW, null, "grace-period");
    }

    @Test
    void usesInjectedClockForNoShowTimestamp() {
        AppointmentRepository repository = mock(AppointmentRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        AppointmentStatusHistoryService history = mock(AppointmentStatusHistoryService.class);
        Instant evaluatedAt = Instant.parse("2030-01-02T07:00:00Z");
        AppointmentScheduler scheduler = new AppointmentScheduler(repository, notifications, history,
                Clock.fixed(evaluatedAt, ZoneId.of("Asia/Qyzylorda")));
        Appointment expired = new Appointment(); expired.setAppointmentAt(LocalDateTime.of(2030, 1, 2, 11, 29)); expired.setStatus(AppointmentStatus.BOOKED);
        when(repository.findAllByAppointmentAtBeforeAndStatusIn(any(), any())).thenReturn(List.of(expired));
        when(repository.findAllByAppointmentAtBetweenAndStatusIn(any(), any(), any())).thenReturn(List.of());

        scheduler.process();

        assertEquals(evaluatedAt, expired.getNoShowAt());
    }
}
