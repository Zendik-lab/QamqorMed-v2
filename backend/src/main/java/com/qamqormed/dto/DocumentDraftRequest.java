package com.qamqormed.dto;

import jakarta.validation.constraints.NotNull;

public record DocumentDraftRequest(@NotNull Long templateId) {}
