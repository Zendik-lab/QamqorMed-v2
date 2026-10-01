package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MedicalCertificateCreateRequest(@NotBlank String type, LocalDate validUntil, String description) {}
