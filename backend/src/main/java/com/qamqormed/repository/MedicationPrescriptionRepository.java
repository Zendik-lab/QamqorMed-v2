package com.qamqormed.repository;

import com.qamqormed.entity.MedicationPrescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicationPrescriptionRepository extends JpaRepository<MedicationPrescription, Long> {
    List<MedicationPrescription> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
}
