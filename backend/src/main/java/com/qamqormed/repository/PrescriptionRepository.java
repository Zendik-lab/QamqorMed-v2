package com.qamqormed.repository;

import com.qamqormed.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
}
