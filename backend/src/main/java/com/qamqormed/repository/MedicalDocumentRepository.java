package com.qamqormed.repository;

import com.qamqormed.entity.MedicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    List<MedicalDocument> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
}
