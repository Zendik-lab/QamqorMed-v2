package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Текущий пароль обязателен") String currentPassword,
        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String newPassword,
        @NotBlank(message = "Подтверждение нового пароля обязательно") String confirmNewPassword
) {
}
