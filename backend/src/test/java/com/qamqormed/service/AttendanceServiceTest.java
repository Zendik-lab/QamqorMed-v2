package com.qamqormed.service;

import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AttendanceServiceTest {
    @Test
    void calculatesAttendanceWithoutCountingAdvanceCancellationsAsMisses() {
        AppointmentRepository repository = mock(AppointmentRepository.class);
        PatientService patients = mock(PatientService.class);
        when(patients.isOwnedDependentId(10L, 10L)).thenReturn(false);
        when(patients.isOwnProfileId(10L, 10L)).thenReturn(true);
        Appointment completed = new Appointment(); completed.setStatus(AppointmentStatus.COMPLETED);
        Appointment cancelled = new Appointment(); cancelled.setStatus(AppointmentStatus.CANCELLED);
        Appointment noShow = new Appointment(); noShow.setStatus(AppointmentStatus.NO_SHOW);
        when(repository.findAllByBookedByIdOrderByAppointmentAtDesc(10L)).thenReturn(List.of(completed, cancelled, noShow));
        var response = new AttendanceService(repository, patients).forSubject(10L, 10L);
        assertEquals(3, response.totalAppointments());
        assertEquals(1, response.completedAppointments());
        assertEquals(1, response.cancelledAppointments());
        assertEquals(1, response.noShowAppointments());
        assertEquals(50, response.attendanceRate());
    }
}
