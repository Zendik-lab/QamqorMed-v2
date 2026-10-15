package com.qamqormed.repository;

import com.qamqormed.entity.DoctorDefaultTemplate;
import com.qamqormed.entity.MedicalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorDefaultTemplateRepository extends JpaRepository<DoctorDefaultTemplate, Long> {
    Optional<DoctorDefaultTemplate> findByDoctorIdAndDocumentType(Long doctorId, MedicalDocumentType documentType);
}
