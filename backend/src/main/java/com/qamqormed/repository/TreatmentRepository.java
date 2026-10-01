package com.qamqormed.repository;

import com.qamqormed.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
}
