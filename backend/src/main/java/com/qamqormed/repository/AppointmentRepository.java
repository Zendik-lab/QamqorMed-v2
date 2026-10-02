package com.qamqormed.repository;

import com.qamqormed.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByBookedByIdOrderByAppointmentAtDesc(Long bookedById);
    Optional<Appointment> findByIdAndBookedById(Long id, Long bookedById);
    List<Appointment> findAllByDoctorIdOrderByAppointmentAtAsc(Long doctorId);
    List<Appointment> findAllByDoctorIdInOrderByAppointmentAtAsc(Collection<Long> doctorIds);
    Optional<Appointment> findByIdAndDoctorId(Long id, Long doctorId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id and a.doctor.id = :doctorId")
    Optional<Appointment> findByIdAndDoctorIdForUpdate(@Param("id") Long id, @Param("doctorId") Long doctorId);
    List<Appointment> findAllByDoctorIdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(Long doctorId, java.time.LocalDateTime from, java.time.LocalDateTime to);
    boolean existsByDoctorIdAndAppointmentAtAndStatusNot(Long doctorId, java.time.LocalDateTime appointmentAt, com.qamqormed.entity.AppointmentStatus status);
    boolean existsByBookedByIdAndNote(Long bookedById, String note);
}
