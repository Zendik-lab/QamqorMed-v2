package com.qamqormed.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AiWaitlistProposalResponse(Long proposalId, String status, Instant expiresAt,
                                         AiAppointmentProposalResponse.SubjectCard subject,
                                         AiAppointmentProposalResponse.ClinicCard clinic,
                                         Long specialtyId, String specialtyName, Long doctorId,
                                         LocalDate desiredDateFrom, LocalDate desiredDateTo,
                                         LocalTime preferredTimeFrom, LocalTime preferredTimeTo,
                                         WaitlistResponse waitlist) {}
