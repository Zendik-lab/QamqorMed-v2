package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MedicalCertificateCreateRequest(@NotBlank String type, String title, LocalDate validFrom, LocalDate validUntil,
                                              String documentNumber, String status, String description) {}
