package com.qamqormed.controller;
import com.qamqormed.dto.RegionResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/regions")
public class RegionController {
    private final MedicalStructureService service;
    public RegionController(MedicalStructureService service) { this.service = service; }
    @GetMapping public List<RegionResponse> list() { return service.publicRegions(); }
    @GetMapping("/{id}") public RegionResponse get(@PathVariable Long id) { return service.publicRegion(id); }
}
