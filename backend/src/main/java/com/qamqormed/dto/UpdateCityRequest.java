package com.qamqormed.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record UpdateCityRequest(@NotBlank @Size(max = 150) String name, @NotNull Long regionId, @NotNull Boolean active) {}
