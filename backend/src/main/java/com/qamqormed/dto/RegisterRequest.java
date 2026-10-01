package com.qamqormed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Имя обязательно")
        @Size(max = 100, message = "Имя слишком длинное")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 100, message = "Фамилия слишком длинная")
        String lastName,

        @NotBlank(message = "Номер телефона обязателен")
        String phone,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password,

        @NotBlank(message = "Подтверждение пароля обязательно")
        String confirmPassword
) {
}
