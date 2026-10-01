package com.qamqormed.repository;

import com.qamqormed.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByBookedByIdOrderByAppointmentAtDesc(Long bookedById);
    Optional<Appointment> findByIdAndBookedById(Long id, Long bookedById);
    List<Appointment> findAllByDoctorIdOrderByAppointmentAtAsc(Long doctorId);
    List<Appointment> findAllByDoctorIdInOrderByAppointmentAtAsc(Collection<Long> doctorIds);
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);
    boolean existsByDoctorIdAndAppointmentAtAndStatusNot(Long doctorId, java.time.LocalDateTime appointmentAt, com.qamqormed.entity.AppointmentStatus status);
    boolean existsByBookedByIdAndNote(Long bookedById, String note);
}
