package com.qamqormed.dto;

import com.qamqormed.entity.MedicalCertificate;
import java.time.Instant;
import java.time.LocalDate;

public record MedicalCertificateResponse(Long id, String type, String title, Instant issuedAt, LocalDate validFrom, LocalDate validUntil,
                                         String documentNumber, String status, String description, Long visitId, String doctorName) {
    public static MedicalCertificateResponse from(MedicalCertificate item) {
        var doctor = item.getDoctor().getUser();
        return new MedicalCertificateResponse(item.getId(), item.getType(), item.getTitle(), item.getIssuedAt(), item.getValidFrom(), item.getValidUntil(),
                item.getDocumentNumber(), item.getStatus(), item.getDescription(), item.getMedicalVisit().getId(), doctor.getLastName() + " " + doctor.getFirstName());
    }
}
