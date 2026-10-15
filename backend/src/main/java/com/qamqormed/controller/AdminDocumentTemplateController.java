package com.qamqormed.controller;

import com.qamqormed.dto.AdminDocumentTemplateRequest;
import com.qamqormed.dto.DocumentTemplateResponse;
import com.qamqormed.dto.DocumentTemplateVersionRequest;
import com.qamqormed.dto.DocumentTemplateVersionResponse;
import com.qamqormed.service.AdminDocumentTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/document-templates")
public class AdminDocumentTemplateController {
    private final AdminDocumentTemplateService service;
    public AdminDocumentTemplateController(AdminDocumentTemplateService service) { this.service = service; }
    @GetMapping public List<DocumentTemplateResponse> list() { return service.list(); }
    @PostMapping public DocumentTemplateResponse create(@Valid @RequestBody AdminDocumentTemplateRequest request) { return service.create(request); }
    @GetMapping("/{templateId}/versions") public List<DocumentTemplateVersionResponse> versions(@PathVariable Long templateId) { return service.versions(templateId); }
    @PostMapping("/{templateId}/versions") public DocumentTemplateVersionResponse version(@PathVariable Long templateId, @Valid @RequestBody DocumentTemplateVersionRequest request) { return service.version(templateId, request); }
    @PatchMapping("/{templateId}/active") public void active(@PathVariable Long templateId, @RequestParam boolean value) { service.archive(templateId, value); }
}
