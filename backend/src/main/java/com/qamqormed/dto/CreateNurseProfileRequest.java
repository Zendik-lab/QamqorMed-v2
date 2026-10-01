package com.qamqormed.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record CreateNurseProfileRequest(@NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
                                        @NotBlank String phone, @NotBlank @Size(min = 8, max = 72) String password, @NotNull Long clinicId) {}
