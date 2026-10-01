package com.qamqormed.dto;
import jakarta.validation.constraints.*;
public record CreateDoctorProfileRequest(@NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
                                         @NotBlank String phone, @NotBlank @Size(min = 8, max = 72) String password,
                                         @NotNull Long specialtyId, @NotNull Long clinicId,
                                         @Min(0) @Max(70) int experienceYears, @Size(max = 2000) String bio, @Size(max = 30) String officeNumber) {}
