package com.qamqormed.repository;

import com.qamqormed.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(Long patientUserId);
    List<MedicalRecord> findAllByDependentIdOrderByCreatedAtDesc(Long dependentId);
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);
}
