package com.qamqormed.data;

import com.qamqormed.entity.PatientProfile;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.repository.PatientProfileRepository;
import com.qamqormed.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class PatientProfileInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PatientProfileRepository profiles;
    public PatientProfileInitializer(UserRepository users, PatientProfileRepository profiles) { this.users = users; this.profiles = profiles; }

    @Override
    @Transactional
    public void run(String... args) {
        for (User user : users.findAll()) {
            if (user.getRole() != Role.PATIENT || profiles.findByUserId(user.getId()).isPresent()) continue;
            PatientProfile profile = new PatientProfile();
            profile.setUser(user); profile.setFirstName(user.getFirstName()); profile.setLastName(user.getLastName());
            profile.setMiddleName(user.getMiddleName()); profile.setBirthDate(user.getDateOfBirth());
            profile.setIin(user.getIin()); profile.setPhone(user.getPhone()); profile.setRegion(user.getPreferredRegion()); profile.setCity(user.getPreferredCity());
            profiles.save(profile);
        }
    }
}
