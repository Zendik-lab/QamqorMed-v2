package com.qamqormed.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoleTestController {

    @GetMapping("/patient/test")
    public Map<String, String> patientTest() {
        return Map.of("message", "Patient endpoint", "role", "PATIENT");
    }

    @GetMapping("/doctor/test")
    public Map<String, String> doctorTest() {
        return Map.of("message", "Doctor endpoint", "role", "DOCTOR");
    }

    @GetMapping("/nurse/test")
    public Map<String, String> nurseTest() {
        return Map.of("message", "Nurse endpoint", "role", "NURSE");
    }

    @GetMapping("/admin/test")
    public Map<String, String> adminTest() {
        return Map.of("message", "Admin endpoint", "role", "ADMIN");
    }
}
