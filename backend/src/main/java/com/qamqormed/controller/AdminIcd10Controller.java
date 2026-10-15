package com.qamqormed.controller;

import com.qamqormed.dto.Icd10ImportResponse;
import com.qamqormed.service.Icd10ImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/icd10")
public class AdminIcd10Controller {
    private final Icd10ImportService service;
    public AdminIcd10Controller(Icd10ImportService service) { this.service = service; }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Icd10ImportResponse importCatalog(@RequestPart MultipartFile file, @RequestParam String source, @RequestParam String sourceVersion) {
        return service.importCsv(file, source, sourceVersion);
    }
}
