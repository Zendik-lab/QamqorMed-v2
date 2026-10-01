package com.qamqormed.dto;

import java.time.LocalDate;

public record MedicationPrescriptionRequest(String medicationName, String dosage, String frequency, String duration, String instructions, LocalDate startDate, LocalDate endDate) {}
