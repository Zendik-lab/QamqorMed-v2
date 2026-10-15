package com.qamqormed.repository;

import com.qamqormed.entity.DocumentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAuditLogRepository extends JpaRepository<DocumentAuditLog, Long> {
    List<DocumentAuditLog> findAllByDocumentIdOrderByCreatedAtAsc(Long documentId);
}
