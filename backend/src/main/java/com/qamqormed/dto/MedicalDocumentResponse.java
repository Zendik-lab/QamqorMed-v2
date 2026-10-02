package com.qamqormed.dto;

import com.qamqormed.entity.MedicalDocument;

import java.time.Instant;

public record MedicalDocumentResponse(Long id, String type, String title, String description,
                                      String filePath, String fileUrl, String fileName, String mimeType, Long fileSize,
                                      Instant createdAt, Instant updatedAt) {
    public static MedicalDocumentResponse from(MedicalDocument document) {
        return new MedicalDocumentResponse(document.getId(), document.getType().name(), document.getTitle(),
                document.getDescription(), document.getFilePath(), document.getFileUrl(), document.getFileName(), document.getMimeType(), document.getFileSize(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
