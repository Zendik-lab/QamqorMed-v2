package com.qamqormed.dto;

import com.qamqormed.entity.MedicalDocument;

import java.time.Instant;
import java.time.LocalDateTime;

public record MedicalDocumentResponse(Long id, String documentNumber, String type, String title, String description,
                                      String filePath, String fileUrl, String fileName, String mimeType, Long fileSize,
                                      String status, String language, Long templateId, Integer templateVersion,
                                      String draftContent, String finalizedContent, Instant approvedAt,
                                      Instant createdAt, Instant updatedAt, Long visitId, LocalDateTime appointmentAt,
                                      String doctorName, String clinicName) {
    public static MedicalDocumentResponse from(MedicalDocument document) {
        return new MedicalDocumentResponse(document.getId(), document.getDocumentNumber(), document.getType().name(), document.getTitle(),
                document.getDescription(), document.getFilePath(), document.getFileUrl(), document.getFileName(), document.getMimeType(), document.getFileSize(),
                document.getStatus() == null ? null : document.getStatus().name(), document.getLanguage(),
                document.getTemplate() == null ? null : document.getTemplate().getId(),
                document.getTemplateVersion() == null ? null : document.getTemplateVersion().getVersionNumber(),
                document.getDraftContent(), document.getFinalizedContent(), document.getApprovedAt(), document.getCreatedAt(), document.getUpdatedAt(),
                document.getMedicalVisit().getId(), document.getMedicalVisit().getAppointment().getAppointmentAt(),
                fullName(document.getDoctorUser()), document.getMedicalVisit().getClinic() == null ? null : document.getMedicalVisit().getClinic().getName());
    }

    /** Patient-facing projection never exposes the editable draft body. */
    public static MedicalDocumentResponse fromPatient(MedicalDocument document) {
        MedicalDocumentResponse value = from(document);
        return new MedicalDocumentResponse(value.id(), value.documentNumber(), value.type(), value.title(), value.description(),
                value.filePath(), value.fileUrl(), value.fileName(), value.mimeType(), value.fileSize(), value.status(), value.language(),
                value.templateId(), value.templateVersion(), null, value.finalizedContent(), value.approvedAt(), value.createdAt(), value.updatedAt(),
                value.visitId(), value.appointmentAt(), value.doctorName(), value.clinicName());
    }

    private static String fullName(com.qamqormed.entity.User user) {
        return user == null ? null : String.join(" ", user.getLastName() == null ? "" : user.getLastName(), user.getFirstName() == null ? "" : user.getFirstName(), user.getMiddleName() == null ? "" : user.getMiddleName()).trim().replaceAll(" +", " ");
    }
}
