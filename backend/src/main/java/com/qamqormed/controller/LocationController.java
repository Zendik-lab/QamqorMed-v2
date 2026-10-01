package com.qamqormed.controller;

import com.qamqormed.dto.CityResponse;
import com.qamqormed.dto.RegionResponse;
import com.qamqormed.service.MedicalStructureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final MedicalStructureService structure;

    public LocationController(MedicalStructureService structure) { this.structure = structure; }

    @GetMapping("/regions")
    public List<RegionResponse> regions() { return structure.publicRegions(); }

    @GetMapping("/regions/{regionId}/cities")
    public List<CityResponse> cities(@PathVariable Long regionId) { return structure.publicCitiesByRegion(regionId); }
}
