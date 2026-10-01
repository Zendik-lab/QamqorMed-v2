package com.qamqormed.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record UpdateRegionRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 50) String code, @NotNull Boolean active) {}
