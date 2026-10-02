package com.qamqormed.dto;

import com.qamqormed.entity.MedicalDocumentType;

public record MedicalDocumentRequest(
        MedicalDocumentType type,
        String title,
        String description,
        String filePath,
        String fileUrl,
        String fileName,
        String mimeType,
        Long fileSize
) {
}
