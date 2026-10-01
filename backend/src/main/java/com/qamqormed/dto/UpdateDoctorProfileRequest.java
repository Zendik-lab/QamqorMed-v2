package com.qamqormed.dto;
import jakarta.validation.constraints.*;
public record UpdateDoctorProfileRequest(@NotNull Long specialtyId, @NotNull Long clinicId,
                                         @Min(0) @Max(70) int experienceYears, @Size(max = 2000) String bio,
                                         @Size(max = 30) String officeNumber, @NotNull Boolean active) {}
