package com.qamqormed.repository;

import com.qamqormed.entity.VisitDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisitDiagnosisRepository extends JpaRepository<VisitDiagnosis, Long> {
    List<VisitDiagnosis> findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(Long medicalVisitId);
}
