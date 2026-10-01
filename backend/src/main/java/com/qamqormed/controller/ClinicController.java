package com.qamqormed.controller;
import com.qamqormed.dto.ClinicResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/clinics")
public class ClinicController {
    private final MedicalStructureService service;
    public ClinicController(MedicalStructureService service) { this.service = service; }
    @GetMapping public List<ClinicResponse> list() { return service.publicClinics(); }
    @GetMapping("/{id}") public ClinicResponse get(@PathVariable Long id) { return service.publicClinic(id); }
    @GetMapping("/by-city/{cityId}") public List<ClinicResponse> byCity(@PathVariable Long cityId) { return service.publicClinicsByCity(cityId); }
    @GetMapping("/by-region/{regionId}") public List<ClinicResponse> byRegion(@PathVariable Long regionId) { return service.publicClinicsByRegion(regionId); }
}
