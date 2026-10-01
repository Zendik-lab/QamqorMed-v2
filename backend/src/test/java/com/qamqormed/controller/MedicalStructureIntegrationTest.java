package com.qamqormed.controller;

import com.qamqormed.entity.*;
import com.qamqormed.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MedicalStructureIntegrationTest {
    private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)");
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired CityRepository cities;
    @Autowired ClinicRepository clinics;
    @Autowired SpecialtyRepository specialties;
    @Autowired DoctorProfileRepository doctors;
    @Autowired NurseProfileRepository nurses;
    @Autowired PasswordEncoder encoder;
    @Autowired NurseIntakeRepository intakes;
    @Autowired MedicalRecordRepository records;
    @Autowired AppointmentRepository appointments;
    @Autowired DependentRepository dependents;
    @Autowired TimeSlotRepository timeSlots;

    @BeforeEach
    void clean() {
        intakes.deleteAll(); records.deleteAll(); appointments.deleteAll(); dependents.deleteAll();
        timeSlots.deleteAll(); doctors.deleteAll(); nurses.deleteAll();
        users.findAll().forEach(user -> { user.setPreferredCity(null); user.setPreferredRegion(null); users.save(user); });
        clinics.deleteAll(); cities.deleteAll(); specialties.deleteAll(); regions.deleteAll(); users.deleteAll();
    }

    @Test
    void patientCanBrowseHierarchyAndDoctors() throws Exception {
        User patient = user("+77000000101", Role.PATIENT);
        Region region = new Region(); region.setName("Жамбылская область"); region.setCode("TEST_ZHAMBYL"); regions.save(region);
        City city = new City(); city.setName("Тараз"); city.setRegion(region); cities.save(city);
        Clinic clinic = new Clinic(); clinic.setName("Demo Clinic"); clinic.setCity(city); clinics.save(clinic);
        Specialty specialty = new Specialty(); specialty.setName("Кардиолог"); specialties.save(specialty);
        User doctorUser = user("+77000000102", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(); profile.setUser(doctorUser); profile.setClinic(clinic); profile.setSpecialty(specialty); profile.setExperienceYears(7); profile.setBio("Demo"); doctors.save(profile);
        String token = login(patient.getPhone());

        mockMvc.perform(get("/api/regions").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("TEST_ZHAMBYL"));
        mockMvc.perform(get("/api/regions/{id}/cities", region.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Тараз"));
        mockMvc.perform(get("/api/cities/{id}/clinics", city.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Demo Clinic"));
        mockMvc.perform(get("/api/doctors").param("specialtyId", specialty.getId().toString()).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].lastName").value("DOCTOR"));
    }

    @Test
    void patientLocationAndScopedDoctorSearchUseTheRealHierarchy() throws Exception {
        User patient = user("+77000000108", Role.PATIENT);
        Region region = new Region(); region.setName("Жамбылская область"); region.setCode("LOCATION_TEST"); regions.save(region);
        City city = new City(); city.setName("Тараз"); city.setRegion(region); cities.save(city);
        Clinic clinic = new Clinic(); clinic.setName("Cardio Clinic"); clinic.setCity(city); clinics.save(clinic);
        Specialty specialty = new Specialty(); specialty.setName("Кардиолог"); specialties.save(specialty);
        User doctorUser = user("+77000000109", Role.DOCTOR); DoctorProfile profile = new DoctorProfile(); profile.setUser(doctorUser); profile.setClinic(clinic); profile.setSpecialty(specialty); profile.setExperienceYears(4); doctors.save(profile);
        String token = login(patient.getPhone());

        mockMvc.perform(put("/api/patient/location").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"regionId\":" + region.getId() + ",\"cityId\":" + city.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cityName").value("Тараз"));
        mockMvc.perform(get("/api/patient/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferredCity.name").value("Тараз"));
        mockMvc.perform(get("/api/specialties/by-city/{cityId}", city.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Кардиолог"));
        mockMvc.perform(get("/api/clinics/search").param("cityId", city.getId().toString()).param("specialtyId", specialty.getId().toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Cardio Clinic"));
        mockMvc.perform(get("/api/doctors/search").param("cityId", city.getId().toString()).param("specialtyId", specialty.getId().toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(profile.getId()));
        profile.setDemo(true); doctors.save(profile);
        mockMvc.perform(get("/api/doctors/{id}/time-slots", profile.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].startsAt").exists());
    }

    @Test
    void adminMutationIsForbiddenForPatientAndDuplicateRegionReturnsConflict() throws Exception {
        User patient = user("+77000000103", Role.PATIENT);
        User admin = user("+77000000104", Role.ADMIN);
        String patientToken = login(patient.getPhone());
        String adminToken = login(admin.getPhone());
        String payload = "{\"name\":\"Алматы\",\"code\":\"ALMATY_TEST\"}";
        mockMvc.perform(post("/api/admin/regions").header("Authorization", "Bearer " + patientToken).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/regions").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/regions").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
    }

    @Test
    void adminCreatesDoctorAndNurseWithServerAssignedRoles() throws Exception {
        User admin = user("+77000000105", Role.ADMIN);
        Region region = new Region(); region.setName("Тестовый регион"); region.setCode("STAFF_TEST"); regions.save(region);
        City city = new City(); city.setName("Тестовый город"); city.setRegion(region); cities.save(city);
        Clinic clinic = new Clinic(); clinic.setName("Staff Demo Clinic"); clinic.setCity(city); clinics.save(clinic);
        Specialty specialty = new Specialty(); specialty.setName("Терапевт"); specialties.save(specialty);
        String token = login(admin.getPhone());
        String doctorPayload = "{\"firstName\":\"Алихан\",\"lastName\":\"Сериков\",\"phone\":\"+77000000106\",\"password\":\"Doctor123!\",\"specialtyId\":" + specialty.getId() + ",\"clinicId\":" + clinic.getId() + ",\"experienceYears\":7,\"bio\":\"Demo\",\"officeNumber\":\"205\"}";
        mockMvc.perform(post("/api/admin/doctors").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(doctorPayload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.firstName").value("Алихан")).andExpect(jsonPath("$.password").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(Role.DOCTOR, users.findByPhone("+77000000106").orElseThrow().getRole());
        String nursePayload = "{\"firstName\":\"Айгерим\",\"lastName\":\"Омарова\",\"phone\":\"+77000000107\",\"password\":\"Nurse123!\",\"clinicId\":" + clinic.getId() + "}";
        mockMvc.perform(post("/api/admin/nurses").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(nursePayload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.phone").value("+77000000107")).andExpect(jsonPath("$.password").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(Role.NURSE, users.findByPhone("+77000000107").orElseThrow().getRole());
    }

    private User user(String phone, Role role) {
        User user = new User(); user.setFirstName("Test"); user.setLastName(role.name()); user.setPhone(phone); user.setPassword(encoder.encode("Password123!")); user.setRole(role); user.setEnabled(true); return users.save(user);
    }
    private String login(String phone) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"" + phone + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = TOKEN.matcher(response); if (!matcher.find()) throw new AssertionError("accessToken missing"); return matcher.group(1);
    }
}
