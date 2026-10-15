package com.qamqormed.repository;

import com.qamqormed.entity.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    List<MedicalDocument> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
    List<MedicalDocument> findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(Long patientUserId);
    List<MedicalDocument> findAllByDependentIdOrderByCreatedAtDesc(Long dependentId);
    Optional<MedicalDocument> findByIdAndDoctorUserId(Long id, Long doctorUserId);
}
