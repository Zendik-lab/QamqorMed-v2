package com.qamqormed.controller;

import com.qamqormed.dto.CityResponse;
import com.qamqormed.dto.ClinicResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MedicalHierarchyController {
    private final MedicalStructureService service;
    public MedicalHierarchyController(MedicalStructureService service) { this.service = service; }
    @GetMapping("/regions/{regionId}/cities") public List<CityResponse> cities(@PathVariable Long regionId) { return service.publicCitiesByRegion(regionId); }
    @GetMapping("/cities/{cityId}/clinics") public List<ClinicResponse> clinics(@PathVariable Long cityId) { return service.publicClinicsByCity(cityId); }
    @GetMapping("/regions/{regionId}/clinics") public List<ClinicResponse> clinicsByRegion(@PathVariable Long regionId) { return service.publicClinicsByRegion(regionId); }
}
