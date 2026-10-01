package com.qamqormed.repository;

import com.qamqormed.entity.MedicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {
    Optional<MedicalVisit> findByAppointmentId(Long appointmentId);
    List<MedicalVisit> findAllByPatientUserIdAndDependentIsNullOrderByCompletedAtDesc(Long patientUserId);
    List<MedicalVisit> findAllByDependentIdOrderByCompletedAtDesc(Long dependentId);
}
