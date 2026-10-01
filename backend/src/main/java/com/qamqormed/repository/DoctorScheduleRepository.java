package com.qamqormed.repository;

import com.qamqormed.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    Optional<DoctorSchedule> findByDoctorId(Long doctorId);
}
