package com.qamqormed.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record AiAppointmentProposalResponse(Long proposalId, String status, Instant expiresAt,
                                            SubjectCard subject, DoctorCard doctor, ClinicCard clinic,
                                            LocalDateTime appointmentAt, Long timeSlotId,
                                            AppointmentResponse appointment) {
    public record SubjectCard(Long id, String type, String fullName) {}
    public record DoctorCard(Long id, String fullName, String specialty) {}
    public record ClinicCard(Long id, String name, String cityName, String regionName) {}
}
