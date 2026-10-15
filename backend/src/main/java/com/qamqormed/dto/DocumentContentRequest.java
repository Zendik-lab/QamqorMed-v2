package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentContentRequest(@NotBlank String content) {}
