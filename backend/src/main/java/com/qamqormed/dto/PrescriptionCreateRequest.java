package com.qamqormed.dto;

import java.time.LocalDate;

public record PrescriptionCreateRequest(String title, String description, String instructions, LocalDate startDate, LocalDate endDate) {}
