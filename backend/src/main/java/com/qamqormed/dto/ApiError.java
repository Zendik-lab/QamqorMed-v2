package com.qamqormed.dto;

import java.util.Map;

public record ApiError(int status, String message, Map<String, String> errors) {

    public ApiError(int status, String message) {
        this(status, message, Map.of());
    }
}
