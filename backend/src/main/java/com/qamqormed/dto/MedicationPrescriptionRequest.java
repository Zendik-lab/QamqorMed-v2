package com.qamqormed.dto;

import java.time.LocalDate;

public record MedicationPrescriptionRequest(String medicationName, String dosage, String dosageUnit, String frequency, String route,
                                             String duration, Integer durationDays, String instructions, LocalDate startDate, LocalDate endDate) {}
