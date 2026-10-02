package com.qamqormed.dto;

import com.qamqormed.entity.LabResult;
import java.time.LocalDate;

public record LabResultResponse(Long id, String testName, String testCode, LocalDate testDate, String status, String result,
                                String referenceRange, String comment, java.time.Instant orderedAt, java.time.Instant completedAt,
                                Long visitId, String doctorName) {
    public static LabResultResponse from(LabResult item) {
        var doctor = item.getDoctor().getUser();
        return new LabResultResponse(item.getId(), item.getTestName(), item.getTestCode(), item.getTestDate(), item.getStatus(), item.getResult(),
                item.getReferenceRange(), item.getComment(), item.getOrderedAt(), item.getCompletedAt(), item.getMedicalVisit() == null ? null : item.getMedicalVisit().getId(),
                doctor.getLastName() + " " + doctor.getFirstName());
    }
}
