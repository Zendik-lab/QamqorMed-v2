package com.qamqormed.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record CreateClinicRequest(@NotBlank @Size(max = 200) String name, @NotNull Long cityId,
                                  @Size(max = 300) String address, @Size(max = 30) String phone, @Size(max = 2000) String description) {}
