package com.qamqormed.controller;
import com.qamqormed.dto.CityResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/cities")
public class CityController {
    private final MedicalStructureService service;
    public CityController(MedicalStructureService service) { this.service = service; }
    @GetMapping public List<CityResponse> list() { return service.publicCities(); }
    @GetMapping("/{id}") public CityResponse get(@PathVariable Long id) { return service.publicCity(id); }
    @GetMapping("/by-region/{regionId}") public List<CityResponse> byRegion(@PathVariable Long regionId) { return service.publicCitiesByRegion(regionId); }
}
