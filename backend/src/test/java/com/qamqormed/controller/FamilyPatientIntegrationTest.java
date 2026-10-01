package com.qamqormed.controller;

import com.qamqormed.entity.City;
import com.qamqormed.entity.Clinic;
import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.MedicalRecord;
import com.qamqormed.entity.Region;
import com.qamqormed.entity.Role;
import com.qamqormed.entity.Specialty;
import com.qamqormed.entity.User;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DependentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.MedicalDocumentRepository;
import com.qamqormed.repository.MedicalVisitRepository;
import com.qamqormed.repository.ReferralRepository;
import com.qamqormed.repository.TreatmentRepository;
import com.qamqormed.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamilyPatientIntegrationTest {
    private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)");
    private static final Pattern ID = Pattern.compile("\\\"id\\\":(\\d+)");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired DependentRepository dependents;
    @Autowired AppointmentRepository appointments;
    @Autowired MedicalRecordRepository records;
    @Autowired MedicalDocumentRepository documents;
    @Autowired MedicalVisitRepository visits;
    @Autowired ReferralRepository referrals;
    @Autowired TreatmentRepository treatments;
    @Autowired DoctorProfileRepository doctors;
    @Autowired com.qamqormed.repository.TimeSlotRepository timeSlots;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        documents.deleteAll();
        referrals.deleteAll();
        treatments.deleteAll();
        visits.deleteAll();
        records.deleteAll();
        appointments.deleteAll();
        timeSlots.deleteAll();
        dependents.deleteAll();
        doctors.deleteAll();
        users.deleteAll();
    }

    @Test
    void separatesSelfAndDependentAppointmentsAndHistoryAndBlocksIdor() throws Exception {
        User parent = user("+77000000201", "Иван", "Иванов", Role.PATIENT);
        User otherParent = user("+77000000202", "Пётр", "Петров", Role.PATIENT);
        User doctorUser = user("+77000000203", "Айдар", "Врач", Role.DOCTOR);
        DoctorProfile doctor = doctor(doctorUser);
        String parentToken = login(parent.getPhone());
        String otherToken = login(otherParent.getPhone());
        String doctorToken = login(doctorUser.getPhone());

        String dependentResponse = mockMvc.perform(post("/api/patient/dependents")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Мария","lastName":"Иванова","dateOfBirth":"2016-05-10","gender":"FEMALE","relationship":"CHILD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(parent.getId()))
                .andReturn().getResponse().getContentAsString();
        Long dependentId = extractId(dependentResponse);

        mockMvc.perform(get("/api/patient/dependents/{id}", dependentId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        String appointmentAt = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0).toString();
        mockMvc.perform(post("/api/patient/appointments")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":" + doctor.getId() + ",\"appointmentAt\":\"" + appointmentAt + "\",\"dependentId\":" + dependentId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject.type").value("DEPENDENT"))
                .andExpect(jsonPath("$.subject.fullName").value("Иванова Мария"))
                .andExpect(jsonPath("$.bookedBy.fullName").value("Иванов Иван"));

        mockMvc.perform(post("/api/patient/appointments")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":" + doctor.getId() + ",\"appointmentAt\":\"" + appointmentAt + "\",\"dependentId\":" + dependentId + "}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/doctor/appointments").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject.fullName").value("Иванова Мария"))
                .andExpect(jsonPath("$[0].bookedBy.fullName").value("Иванов Иван"));

        Long appointmentId = appointments.findAll().get(0).getId();
        mockMvc.perform(post("/api/doctor/appointments/{id}/medical-record", appointmentId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"ОРВИ\",\"recommendations\":\"Покой\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.fullName").value("Иванова Мария"));

        mockMvc.perform(get("/api/patient/medical-records").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/patient/dependents/{id}/medical-records", dependentId).header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].diagnosis").value("ОРВИ"));

        mockMvc.perform(post("/api/patient/appointments")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":" + doctor.getId() + ",\"appointmentAt\":\"" + LocalDateTime.now().plusDays(4).withSecond(0).withNano(0) + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.subject.type").value("SELF"));
    }

    private User user(String phone, String firstName, String lastName, Role role) {
        User user = new User(); user.setFirstName(firstName); user.setLastName(lastName); user.setPhone(phone);
        user.setPassword(encoder.encode("Password123!")); user.setRole(role); user.setEnabled(true); return users.save(user);
    }

    private DoctorProfile doctor(User user) {
        Region region = new Region(); region.setName("Тестовый регион"); region.setCode("FAMILY_TEST");
        City city = new City(); city.setName("Тестовый город"); city.setRegion(region);
        Clinic clinic = new Clinic(); clinic.setName("Family Clinic"); clinic.setCity(city);
        Specialty specialty = new Specialty(); specialty.setName("Терапевт");
        region = saveRegion(region); city.setRegion(region); city = saveCity(city); clinic.setCity(city); clinic = saveClinic(clinic); specialty = saveSpecialty(specialty);
        DoctorProfile profile = new DoctorProfile(); profile.setUser(user); profile.setClinic(clinic); profile.setSpecialty(specialty); profile.setExperienceYears(5);
        return doctors.save(profile);
    }

    @Autowired com.qamqormed.repository.RegionRepository regions;
    @Autowired com.qamqormed.repository.CityRepository cities;
    @Autowired com.qamqormed.repository.ClinicRepository clinics;
    @Autowired com.qamqormed.repository.SpecialtyRepository specialties;
    private Region saveRegion(Region value) { return regions.save(value); }
    private City saveCity(City value) { return cities.save(value); }
    private Clinic saveClinic(Clinic value) { return clinics.save(value); }
    private Specialty saveSpecialty(Specialty value) { return specialties.save(value); }

    private String login(String phone) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = TOKEN.matcher(body); if (!matcher.find()) throw new AssertionError("accessToken missing"); return matcher.group(1);
    }

    private Long extractId(String body) {
        Matcher matcher = ID.matcher(body); if (!matcher.find()) throw new AssertionError("id missing"); return Long.valueOf(matcher.group(1));
    }
}
