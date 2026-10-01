package com.qamqormed.controller;

import com.qamqormed.dto.ICD10CodeResponse;
import com.qamqormed.service.ClinicalStructuredService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/icd10")
public class ClinicalCatalogController {
    private final ClinicalStructuredService service;
    public ClinicalCatalogController(ClinicalStructuredService service) { this.service = service; }
    @GetMapping public List<ICD10CodeResponse> search(@RequestParam(required = false) String query) { return service.searchCodes(query); }
}
