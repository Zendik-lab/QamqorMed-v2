package com.qamqormed.repository;

import com.qamqormed.entity.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, Long> {
    List<MedicalCertificate> findAllByPatientProfileIdOrderByIssuedAtDesc(Long patientProfileId);
    List<MedicalCertificate> findAllByDependentIdOrderByIssuedAtDesc(Long dependentId);
    List<MedicalCertificate> findAllByMedicalVisitIdOrderByIssuedAtDesc(Long medicalVisitId);
}
