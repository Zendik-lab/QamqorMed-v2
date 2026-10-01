package com.qamqormed.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateSpecialtyRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String description) {}
