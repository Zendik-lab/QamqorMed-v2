package com.qamqormed.controller;

import com.qamqormed.entity.Role;
import com.qamqormed.entity.User;
import com.qamqormed.repository.UserRepository;
import com.qamqormed.repository.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class DevToolsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private SpecialtyRepository specialties;

    @Test
    void searchesDemoDoctorsAndReturnsTheirRealAssignment() throws Exception {
        mockMvc.perform(get("/api/dev/users/search")
                        .param("role", "DOCTOR")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("DOCTOR"))
                .andExpect(jsonPath("$.content[0].demo").value(true))
                .andExpect(jsonPath("$.content[0].relation.connected").value(true))
                .andExpect(jsonPath("$.content[0].credentials.phone").isString())
                .andExpect(jsonPath("$.content[0].credentials.password").isString())
                .andExpect(jsonPath("$.content[0].relation.assignmentId").isNumber());
    }

    @Test
    void searchesDemoNursesByAssignedDoctorSpecialty() throws Exception {
        Long specialtyId = specialties.findAllByActiveTrueOrderByNameAsc().get(0).getId();

        mockMvc.perform(get("/api/dev/users/search")
                        .param("role", "NURSE")
                        .param("specialtyId", specialtyId.toString())
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("NURSE"))
                .andExpect(jsonPath("$.content[0].specialty.id").value(specialtyId.intValue()))
                .andExpect(jsonPath("$.content[0].relation.connected").value(true));
    }

    @Test
    void quickLoginUsesTheNormalAuthResponseForAValidatedDemoUser() throws Exception {
        Long userId = users.findByPhone("+77000000002").orElseThrow().getId();

        mockMvc.perform(post("/api/dev/login/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.user.role").value("DOCTOR"));
    }

    @Test
    void quickLoginDoesNotExposeAnEnabledNonDemoAccount() throws Exception {
        User realPatient = new User();
        realPatient.setFirstName("Private");
        realPatient.setLastName("Patient");
        realPatient.setPhone("+77009999999");
        realPatient.setPassword("already-hashed");
        realPatient.setRole(Role.PATIENT);
        realPatient.setEnabled(true);
        realPatient = users.saveAndFlush(realPatient);

        mockMvc.perform(get("/api/dev/users/search")
                        .param("role", "PATIENT")
                        .param("query", "Private"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(post("/api/dev/login/{userId}", realPatient.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void workflowPanelSearchesDemoAppointmentsAndRunsTheRealScheduler() throws Exception {
        String response = mockMvc.perform(get("/api/dev/notification-workflow/appointments")
                        .with(user("dev-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].demo").value(true))
                .andExpect(jsonPath("$[0].status").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();
        long appointmentId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get(0).get("id").longValue();

        mockMvc.perform(post("/api/dev/notification-workflow/appointments/{id}/reminder-24h", appointmentId)
                        .with(user("dev-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointment.id").value((int) appointmentId))
                .andExpect(jsonPath("$.lastAction.status").value("PASS"))
                .andExpect(jsonPath("$.lastAction.action").value("reminder-24h"))
                .andExpect(jsonPath("$.scheduler.evaluatedAt").isString())
                .andExpect(jsonPath("$.notifications[0].type").value("APPOINTMENT_CONFIRMATION_REQUIRED"));
    }

    @Test
    void appointmentInventoryIsPaginatedAndDoesNotRequireDemoMarkerForReadOnlySearch() throws Exception {
        mockMvc.perform(get("/api/dev/appointments")
                        .with(user("dev-admin").roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].createdAt").isString())
                .andExpect(jsonPath("$.content[0].cityName").isString())
                .andExpect(jsonPath("$.content[0].regionName").isString());
    }

    @Test
    void appointmentInventoryRejectsAnonymousAndPatientUsers() throws Exception {
        mockMvc.perform(get("/api/dev/appointments"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dev/appointments")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isForbidden());
    }
}
