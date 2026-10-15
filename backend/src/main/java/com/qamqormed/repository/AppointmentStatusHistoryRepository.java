package com.qamqormed.repository;
import com.qamqormed.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {
    List<AppointmentStatusHistory> findAllByAppointmentIdOrderByChangedAtAsc(Long appointmentId);
}
