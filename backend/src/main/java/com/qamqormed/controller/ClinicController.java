package com.qamqormed.controller;
import com.qamqormed.dto.ClinicResponse;
import com.qamqormed.dto.PageResponse;
import com.qamqormed.dto.SpecialtyResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/clinics")
public class ClinicController {
    private final MedicalStructureService service;
    public ClinicController(MedicalStructureService service) { this.service = service; }
    @GetMapping public List<ClinicResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.publicClinicsPage(page, size).content(); }
    @GetMapping("/page") public PageResponse<ClinicResponse> page(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.publicClinicsPage(page, size); }
    @GetMapping("/{id}") public ClinicResponse get(@PathVariable Long id) { return service.publicClinic(id); }
    @GetMapping("/by-city/{cityId}") public List<ClinicResponse> byCity(@PathVariable Long cityId) { return service.publicClinicsByCity(cityId); }
    @GetMapping("/by-region/{regionId}") public List<ClinicResponse> byRegion(@PathVariable Long regionId) { return service.publicClinicsByRegion(regionId); }
    @GetMapping("/{clinicId}/specialties") public List<SpecialtyResponse> specialties(@PathVariable Long clinicId) { return service.publicSpecialtiesByClinic(clinicId); }
    @GetMapping("/search") public List<ClinicResponse> search(@RequestParam Long cityId, @RequestParam Long specialtyId) {
        return service.publicClinicsByCityAndSpecialty(cityId, specialtyId);
    }
}
