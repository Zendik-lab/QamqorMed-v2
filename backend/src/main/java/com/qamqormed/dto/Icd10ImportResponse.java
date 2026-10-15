package com.qamqormed.dto;

public record Icd10ImportResponse(int imported, int updated, int skipped, String source, String sourceVersion) {}
