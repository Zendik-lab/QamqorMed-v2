package com.qamqormed.repository;

import com.qamqormed.entity.DocumentTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateVersionRepository extends JpaRepository<DocumentTemplateVersion, Long> {
    List<DocumentTemplateVersion> findAllByTemplateIdOrderByVersionNumberDesc(Long templateId);
    Optional<DocumentTemplateVersion> findTopByTemplateIdOrderByVersionNumberDesc(Long templateId);
}
