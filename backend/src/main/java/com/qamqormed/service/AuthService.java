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
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.NurseProfileRepository;
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
    private final DoctorProfileRepository doctorProfiles;
    private final NurseProfileRepository nurseProfiles;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       DoctorProfileRepository doctorProfiles, NurseProfileRepository nurseProfiles) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.doctorProfiles = doctorProfiles;
        this.nurseProfiles = nurseProfiles;
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
        return response(userRepository.save(user));
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

        return new AuthResponse(jwtService.generateToken(user), "Bearer", response(user));
    }

    /**
     * Builds the same authentication response as the regular login flow for a
     * user that has already been validated by a development-only service.
     */
    @Transactional(readOnly = true)
    public AuthResponse issueTokenForValidatedUser(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", response(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Требуется авторизация"));
        return response(user);
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

    private UserResponse response(User user) {
        if (user.getRole() == Role.DOCTOR) {
            return doctorProfiles.findByUserId(user.getId())
                    .map(profile -> UserResponse.from(
                            user,
                            new UserResponse.SpecialtySummary(profile.getSpecialty().getId(), profile.getSpecialty().getName()),
                            clinic(profile.getClinic())
                    ))
                    .orElseGet(() -> UserResponse.from(user));
        }
        if (user.getRole() == Role.NURSE) {
            return nurseProfiles.findByUserId(user.getId())
                    .map(profile -> UserResponse.from(user, null, clinic(profile.getClinic())))
                    .orElseGet(() -> UserResponse.from(user));
        }
        return UserResponse.from(user);
    }

    private UserResponse.ClinicSummary clinic(com.qamqormed.entity.Clinic clinic) {
        var city = clinic.getCity();
        return new UserResponse.ClinicSummary(
                clinic.getId(),
                clinic.getName(),
                city.getName(),
                city.getRegion().getName()
        );
    }
}
