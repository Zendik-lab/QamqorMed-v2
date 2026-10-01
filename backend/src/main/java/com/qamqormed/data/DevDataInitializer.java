package com.qamqormed.data;

import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing("Demo", "Patient", "+77000000001", "Patient123!", Role.PATIENT);
        createIfMissing("Demo", "Doctor", "+77000000002", "Doctor123!", Role.DOCTOR);
        createIfMissing("Demo", "Nurse", "+77000000003", "Nurse123!", Role.NURSE);
        createIfMissing("Demo", "Admin", "+77000000004", "Admin123!", Role.ADMIN);
    }

    private void createIfMissing(String firstName, String lastName, String phone, String rawPassword, Role role) {
        if (userRepository.existsByPhone(phone)) {
            return;
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
