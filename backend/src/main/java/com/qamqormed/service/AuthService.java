package com.qamqormed.service;

import com.qamqormed.dto.AuthResponse;
import com.qamqormed.dto.ChangePasswordRequest;
import com.qamqormed.dto.LoginRequest;
import com.qamqormed.dto.RegisterRequest;
import com.qamqormed.dto.UserResponse;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.UnauthorizedException;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.security.JwtService;
import com.qamqormed.util.PhoneNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Неверный номер телефона или пароль";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String phone = normalizePhone(request.phone());
        validatePassword(request.password(), request.confirmPassword(), "confirmPassword");

        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Пользователь с таким номером уже существует");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        final String phone;
        try {
            phone = PhoneNormalizer.normalize(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = userRepository.findByPhone(phone)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        return new AuthResponse(jwtService.generateToken(user), "Bearer", UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Требуется авторизация"));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Требуется авторизация"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Текущий пароль указан неверно");
        }
        validatePassword(request.newPassword(), request.confirmNewPassword(), "confirmNewPassword");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private String normalizePhone(String phone) {
        try {
            return PhoneNormalizer.normalize(phone);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Validation failed", Map.of("phone", "Некорректный номер телефона"));
        }
    }

    private void validatePassword(String password, String confirmation, String confirmationField) {
        if (password == null || !password.matches(".*[A-Za-zА-Яа-я].*")) {
            throw new BadRequestException("Validation failed", Map.of("password", "Пароль должен содержать букву"));
        }
        if (!password.matches(".*\\d.*")) {
            throw new BadRequestException("Validation failed", Map.of("password", "Пароль должен содержать цифру"));
        }
        if (!password.equals(confirmation)) {
            throw new BadRequestException("Validation failed", Map.of(confirmationField, "Пароли не совпадают"));
        }
    }
}
