package com.qamqormed.repository;

import com.qamqormed.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    List<LabResult> findAllByPatientProfileIdOrderByTestDateDesc(Long patientProfileId);
    List<LabResult> findAllByDependentIdOrderByTestDateDesc(Long dependentId);
    List<LabResult> findAllByMedicalVisitIdOrderByTestDateDesc(Long medicalVisitId);
}
