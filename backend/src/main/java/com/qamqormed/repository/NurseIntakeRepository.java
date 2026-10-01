package com.qamqormed.repository;

import com.qamqormed.entity.NurseIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NurseIntakeRepository extends JpaRepository<NurseIntake, Long> {
    Optional<NurseIntake> findByAppointmentId(Long appointmentId);
}
