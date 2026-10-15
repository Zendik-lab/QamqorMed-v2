package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        Long conversationId,
        @NotBlank(message = "Введите сообщение") @Size(max = 4000, message = "Сообщение слишком длинное") String message,
        @Size(max = 10, message = "Некорректный язык") String language
) {}
