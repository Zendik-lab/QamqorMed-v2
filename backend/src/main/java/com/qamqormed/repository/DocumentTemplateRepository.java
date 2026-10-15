package com.qamqormed.repository;

import com.qamqormed.entity.DocumentTemplate;
import com.qamqormed.entity.MedicalDocumentType;
import com.qamqormed.entity.TemplateLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    @Query("select t from DocumentTemplate t where t.active = true and (t.scope = com.qamqormed.entity.TemplateScope.SYSTEM or (t.scope = com.qamqormed.entity.TemplateScope.PERSONAL and t.ownerDoctor.id = :doctorId) or (t.scope = com.qamqormed.entity.TemplateScope.CLINIC and t.clinic.id = :clinicId)) and (:type is null or t.documentType = :type) and (:language is null or t.language = :language) and (t.specialty is null or t.specialty.id = :specialtyId) order by t.name")
    List<DocumentTemplate> findVisible(@Param("doctorId") Long doctorId, @Param("clinicId") Long clinicId,
                                        @Param("specialtyId") Long specialtyId, @Param("type") MedicalDocumentType type,
                                        @Param("language") TemplateLanguage language);

    List<DocumentTemplate> findAllByOwnerDoctorIdAndActiveTrueOrderByNameAsc(Long doctorId);
}
