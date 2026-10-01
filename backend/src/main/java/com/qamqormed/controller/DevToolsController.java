package com.qamqormed.controller;

import com.qamqormed.dto.AuthResponse;
import com.qamqormed.dto.DataIntegrityAuditResponse;
import com.qamqormed.dto.CityResponse;
import com.qamqormed.dto.ClinicResponse;
import com.qamqormed.dto.DevRelationResponse;
import com.qamqormed.dto.DevUserResponse;
import com.qamqormed.dto.PageResponse;
import com.qamqormed.dto.RegionResponse;
import com.qamqormed.dto.SpecialtyResponse;
import com.qamqormed.service.DevToolsService;
import com.qamqormed.service.DataIntegrityAuditService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("dev")
@RequestMapping("/api/dev")
public class DevToolsController {
    private final DevToolsService service;
    private final DataIntegrityAuditService auditService;

    public DevToolsController(DevToolsService service, DataIntegrityAuditService auditService) {
        this.service = service; this.auditService = auditService;
    }

    @GetMapping("/users/search")
    public PageResponse<DevUserResponse> search(
            @RequestParam(defaultValue = "DOCTOR") String role,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) String issue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(role, query, regionId, cityId, clinicId, specialtyId, issue, page, size);
    }

    @GetMapping("/users/{userId}/relation")
    public DevRelationResponse relation(@PathVariable Long userId) {
        return service.relationForUser(userId);
    }

    @PostMapping("/login/{userId}")
    public AuthResponse login(@PathVariable Long userId) {
        return service.loginAsDevUser(userId);
    }

    @GetMapping("/filters/regions")
    public List<RegionResponse> regions() {
        return service.regions();
    }

    @GetMapping("/filters/regions/{regionId}/cities")
    public List<CityResponse> cities(@PathVariable Long regionId) {
        return service.cities(regionId);
    }

    @GetMapping("/filters/cities/{cityId}/clinics")
    public List<ClinicResponse> clinics(@PathVariable Long cityId) {
        return service.clinics(cityId, null);
    }

    @GetMapping("/filters/clinics/{clinicId}/specialties")
    public List<SpecialtyResponse> specialties(@PathVariable Long clinicId) {
        return service.specialties(clinicId);
    }

    @GetMapping("/filters/specialties")
    public List<SpecialtyResponse> allSpecialties() {
        return service.specialties(null);
    }

    @GetMapping("/audit")
    public DataIntegrityAuditResponse audit() {
        return auditService.audit();
    }
}
