package com.qamqormed.repository;

import com.qamqormed.entity.ScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    List<ScheduleException> findAllByScheduleDoctorIdAndDateGreaterThanEqualAndDateLessThanOrderByDateAsc(Long doctorId, LocalDate from, LocalDate to);
}
