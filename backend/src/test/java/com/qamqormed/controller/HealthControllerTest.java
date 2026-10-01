package com.qamqormed.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsHealthPayloadAndCorsHeader() throws Exception {
        mockMvc.perform(get("/api/health").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(content().json("""
                        {
                          "status": "UP",
                          "service": "QamqorMed Backend"
                        }
                        """));
    }
}
