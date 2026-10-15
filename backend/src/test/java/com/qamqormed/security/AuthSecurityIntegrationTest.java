package com.qamqormed.security;

import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.NurseProfileRepository;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DependentRepository;
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.NurseIntakeRepository;
import com.qamqormed.repository.MedicalDocumentRepository;
import com.qamqormed.repository.MedicalVisitRepository;
import com.qamqormed.repository.ReferralRepository;
import com.qamqormed.repository.TreatmentRepository;
import com.qamqormed.repository.VisitDiagnosisRepository;
import com.qamqormed.repository.PrescriptionRepository;
import com.qamqormed.repository.MedicationPrescriptionRepository;
import com.qamqormed.repository.LabResultRepository;
import com.qamqormed.repository.MedicalCertificateRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    private static final String PATIENT_PHONE = "+77001234567";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private NurseProfileRepository nurseProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NurseIntakeRepository nurseIntakes;
    @Autowired
    private MedicalDocumentRepository medicalDocuments;
    @Autowired
    private MedicalVisitRepository medicalVisits;
    @Autowired
    private ReferralRepository referrals;
    @Autowired
    private TreatmentRepository treatments;
    @Autowired
    private MedicalRecordRepository medicalRecords;
    @Autowired
    private AppointmentRepository appointments;
    @Autowired
    private DependentRepository dependents;
    @Autowired
    private VisitDiagnosisRepository visitDiagnoses;
    @Autowired
    private PrescriptionRepository prescriptions;
    @Autowired
    private MedicationPrescriptionRepository medicationPrescriptions;
    @Autowired
    private LabResultRepository labResults;
    @Autowired
    private MedicalCertificateRepository certificates;

    @BeforeEach
    void cleanUsers() {
        medicalDocuments.deleteAll();
        referrals.deleteAll();
        treatments.deleteAll();
        visitDiagnoses.deleteAll();
        prescriptions.deleteAll();
        medicationPrescriptions.deleteAll();
        labResults.deleteAll();
        certificates.deleteAll();
        medicalVisits.deleteAll();
        nurseIntakes.deleteAll();
        medicalRecords.deleteAll();
        appointments.deleteAll();
        dependents.deleteAll();
        doctorProfileRepository.deleteAll();
        nurseProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registersPatientAndStoresOnlyBcryptHash() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "Patient",
                                  "phone": "8 700 123 45 67",
                                  "password": "Password123!",
                                  "confirmPassword": "Password123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User user = userRepository.findByPhone(PATIENT_PHONE).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("Password123!", user.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Password123!", user.getPassword()));
        org.junit.jupiter.api.Assertions.assertTrue(user.getPassword().startsWith("$2"));
    }

    @Test
    void requiresAuthenticationAndEnforcesRole() throws Exception {
        registerPatient();
        String patientToken = login(PATIENT_PHONE, "Password123!");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(PATIENT_PHONE))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/patient/test").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor/test").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/nurse/test").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        createUser("+77001234569", Role.DOCTOR);
        String doctorToken = login("+77001234569", "Password123!");
        mockMvc.perform(get("/api/doctor/test").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        User disabledUser = new User();
        disabledUser.setFirstName("Disabled");
        disabledUser.setLastName("User");
        disabledUser.setPhone("+77001234568");
        disabledUser.setPassword(passwordEncoder.encode("Password123!"));
        disabledUser.setRole(Role.PATIENT);
        disabledUser.setEnabled(false);
        userRepository.save(disabledUser);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+77001234568","password":"Password123!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Неверный номер телефона или пароль"));
    }

    @Test
    void rejectsUnconfiguredLanOriginBeforeLogin() throws Exception {
        registerPatient();

        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://192.168.1.140:5173")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+77001234567\",\"password\":\"Password123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aiHistoryRequiresAuthenticationAndIsolatedByOwner() throws Exception {
        registerPatient();
        String firstToken = login(PATIENT_PHONE, "Password123!");

        String conversationBody = mockMvc.perform(post("/api/ai/conversations")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        long conversationId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(conversationBody).path("id").asLong();

        createUser("+77001234569", Role.PATIENT);
        String secondToken = login("+77001234569", "Password123!");

        mockMvc.perform(get("/api/ai/conversations")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(get("/api/ai/conversations/{id}/messages", conversationId)
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/ai/conversations/{id}", conversationId)
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ai/conversations"))
                .andExpect(status().isUnauthorized());
    }

    private void registerPatient() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"Patient","phone":"+77001234567","password":"Password123!","confirmPassword":"Password123!"}
                                """))
                .andExpect(status().isCreated());
    }

    private void createUser(String phone, Role role) {
        User user = new User();
        user.setFirstName("Demo");
        user.setLastName(role.name());
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String login(String phone, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher matcher = TOKEN_PATTERN.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Login response did not contain accessToken");
        }
        return matcher.group(1);
    }
}
