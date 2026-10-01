package com.qamqormed.dto;

import com.qamqormed.entity.MedicalCertificate;
import java.time.Instant;
import java.time.LocalDate;

public record MedicalCertificateResponse(Long id, String type, Instant issuedAt, LocalDate validUntil,
                                         String description, Long visitId, String doctorName) {
    public static MedicalCertificateResponse from(MedicalCertificate item) {
        var doctor = item.getDoctor().getUser();
        return new MedicalCertificateResponse(item.getId(), item.getType(), item.getIssuedAt(), item.getValidUntil(),
                item.getDescription(), item.getMedicalVisit().getId(), doctor.getLastName() + " " + doctor.getFirstName());
    }
}
