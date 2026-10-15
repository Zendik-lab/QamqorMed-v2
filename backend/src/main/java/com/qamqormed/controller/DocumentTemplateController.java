package com.qamqormed.controller;

import com.qamqormed.dto.DocumentTemplateCreateRequest;
import com.qamqormed.dto.DocumentTemplateResponse;
import com.qamqormed.dto.DocumentTemplateVersionRequest;
import com.qamqormed.dto.DocumentTemplateVersionResponse;
import com.qamqormed.entity.MedicalDocumentType;
import com.qamqormed.security.AuthenticatedUser;
import com.qamqormed.service.DocumentTemplateService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/document-templates")
public class DocumentTemplateController {
    private final DocumentTemplateService service;

    public DocumentTemplateController(DocumentTemplateService service) { this.service = service; }

    @GetMapping
    public List<DocumentTemplateResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam(required = false) MedicalDocumentType type,
                                               @RequestParam(required = false) String language) {
        return service.visible(user.getId(), type, language);
    }

    @PostMapping
    public DocumentTemplateResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody DocumentTemplateCreateRequest request) {
        return service.createPersonal(user.getId(), request);
    }

    @GetMapping("/{templateId}/versions")
    public List<DocumentTemplateVersionResponse> versions(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable Long templateId) {
        return service.versions(user.getId(), templateId);
    }

    @PostMapping("/{templateId}/versions")
    public DocumentTemplateVersionResponse version(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long templateId,
                                                   @Valid @RequestBody DocumentTemplateVersionRequest request) {
        return service.createVersion(user.getId(), templateId, request);
    }

    @PostMapping("/{templateId}/favorite")
    public void favorite(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long templateId) {
        service.favorite(user.getId(), templateId, true);
    }

    @DeleteMapping("/{templateId}/favorite")
    public void unfavorite(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long templateId) {
        service.favorite(user.getId(), templateId, false);
    }

    @PutMapping("/defaults/{type}")
    public void setDefault(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable MedicalDocumentType type,
                           @RequestParam Long templateId) {
        service.setDefault(user.getId(), type, templateId);
    }
}
