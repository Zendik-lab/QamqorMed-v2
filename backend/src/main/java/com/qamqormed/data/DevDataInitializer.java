package com.qamqormed.data;

import com.qamqormed.entity.*;
import com.qamqormed.repository.*;
import com.qamqormed.service.StaffProfileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;
    private final ClinicRepository clinicRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final NurseProfileRepository nurseProfileRepository;
    private final StaffProfileService staffProfileService;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, RegionRepository regionRepository,
                              CityRepository cityRepository, ClinicRepository clinicRepository, SpecialtyRepository specialtyRepository,
                              DoctorProfileRepository doctorProfileRepository, NurseProfileRepository nurseProfileRepository,
                              StaffProfileService staffProfileService) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.regionRepository = regionRepository;
        this.cityRepository = cityRepository; this.clinicRepository = clinicRepository; this.specialtyRepository = specialtyRepository;
        this.doctorProfileRepository = doctorProfileRepository; this.nurseProfileRepository = nurseProfileRepository; this.staffProfileService = staffProfileService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createIfMissing("Demo", "Patient", "+77000000001", "Patient123!", Role.PATIENT);
        User doctorUser = createIfMissing("Demo", "Doctor", "+77000000002", "Doctor123!", Role.DOCTOR);
        User nurseUser = createIfMissing("Demo", "Nurse", "+77000000003", "Nurse123!", Role.NURSE);
        createIfMissing("Demo", "Admin", "+77000000004", "Admin123!", Role.ADMIN);
        Region region = regionRepository.findByCodeIgnoreCase("ZHAMBYL").orElseGet(() -> {
            Region value = new Region(); value.setName("Жамбылская область"); value.setCode("ZHAMBYL"); value.setActive(true); return regionRepository.save(value);
        });
        City city = cityRepository.findByNameIgnoreCaseAndRegionId("Тараз", region.getId()).orElseGet(() -> {
            City value = new City(); value.setName("Тараз"); value.setRegion(region); value.setActive(true); return cityRepository.save(value);
        });
        Clinic clinic = clinicRepository.findByNameIgnoreCaseAndCityId("QamqorMed Demo Clinic", city.getId()).orElseGet(() -> {
            Clinic value = new Clinic(); value.setName("QamqorMed Demo Clinic"); value.setCity(city); value.setAddress("Демо-адрес, Тараз"); value.setPhone("+77260000000"); value.setDescription("Учебная демонстрационная клиника QamqorMed"); value.setActive(true); return clinicRepository.save(value);
        });
        Specialty cardiologist = specialty("Кардиолог", "Демонстрационная специальность");
        specialty("Терапевт", "Демонстрационная специальность");
        specialty("Педиатр", "Демонстрационная специальность");
        if (doctorUser.getRole() == Role.DOCTOR && doctorProfileRepository.findByUserId(doctorUser.getId()).isEmpty()) {
            staffProfileService.attachExistingDoctor(doctorUser, cardiologist, clinic, 7, "Демонстрационный профиль врача", "205");
        }
        if (nurseUser.getRole() == Role.NURSE && nurseProfileRepository.findByUserId(nurseUser.getId()).isEmpty()) {
            staffProfileService.attachExistingNurse(nurseUser, clinic);
        }
    }

    private User createIfMissing(String firstName, String lastName, String phone, String rawPassword, Role role) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            User user = new User(); user.setFirstName(firstName); user.setLastName(lastName); user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(rawPassword)); user.setRole(role); user.setEnabled(true); return userRepository.save(user);
        });
    }

    private Specialty specialty(String name, String description) {
        return specialtyRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Specialty value = new Specialty(); value.setName(name); value.setDescription(description); value.setActive(true); return specialtyRepository.save(value);
        });
    }
}
