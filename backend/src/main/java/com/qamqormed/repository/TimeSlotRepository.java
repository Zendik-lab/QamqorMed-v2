package com.qamqormed.repository;

import com.qamqormed.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    boolean existsByDoctorIdAndStartsAt(Long doctorId, LocalDateTime startsAt);
    Optional<TimeSlot> findByDoctorIdAndStartsAt(Long doctorId, LocalDateTime startsAt);

    @Query("select t from TimeSlot t where t.doctor.id = :doctorId and t.active = true and t.startsAt >= :fromAt and t.startsAt < :toAt and not exists (select a.id from Appointment a where a.doctor.id = t.doctor.id and a.appointmentAt = t.startsAt and a.status <> com.qamqormed.entity.AppointmentStatus.CANCELLED) order by t.startsAt")
    List<TimeSlot> findAvailable(Long doctorId, LocalDateTime fromAt, LocalDateTime toAt);
}
