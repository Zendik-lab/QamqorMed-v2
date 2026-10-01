package com.qamqormed.dto;
import jakarta.validation.constraints.NotNull;
public record UpdateNurseProfileRequest(@NotNull Long clinicId, @NotNull Boolean active) {}
