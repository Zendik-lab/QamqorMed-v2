package com.qamqormed.controller;
import com.qamqormed.dto.SpecialtyResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {
    private final MedicalStructureService service;
    public SpecialtyController(MedicalStructureService service) { this.service = service; }
    @GetMapping public List<SpecialtyResponse> list() { return service.publicSpecialties(); }
    @GetMapping("/by-city/{cityId}") public List<SpecialtyResponse> byCity(@PathVariable Long cityId) { return service.publicSpecialtiesByCity(cityId); }
    @GetMapping("/{id}") public SpecialtyResponse get(@PathVariable Long id) { return service.publicSpecialty(id); }
}
