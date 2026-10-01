package com.qamqormed.controller;

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
}
