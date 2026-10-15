package com.qamqormed.service;

import com.qamqormed.dto.AdminDocumentTemplateRequest;
import com.qamqormed.dto.DocumentTemplateResponse;
import com.qamqormed.dto.DocumentTemplateVersionRequest;
import com.qamqormed.dto.DocumentTemplateVersionResponse;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDocumentTemplateService {
    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final UserRepository users;
    private final TemplateRenderer renderer;

    public AdminDocumentTemplateService(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions,
                                        ClinicRepository clinics, SpecialtyRepository specialties, UserRepository users,
                                        TemplateRenderer renderer) {
        this.templates = templates; this.versions = versions; this.clinics = clinics; this.specialties = specialties; this.users = users; this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateResponse> list() { return templates.findAll().stream().map(item -> DocumentTemplateResponse.from(item, false, false)).toList(); }

    @Transactional
    public DocumentTemplateResponse create(AdminDocumentTemplateRequest request) {
        if (request.scope() == TemplateScope.PERSONAL) throw new BadRequestException("Личные шаблоны создаются врачом");
        if (request.scope() == TemplateScope.CLINIC && request.clinicId() == null) throw new BadRequestException("Для клинического шаблона укажите клинику");
        DocumentTemplate template = new DocumentTemplate(); template.setName(request.name().trim()); template.setDocumentType(request.documentType()); template.setScope(request.scope()); template.setLanguage(request.language());
        if (request.scope() == TemplateScope.CLINIC) template.setClinic(clinics.findById(request.clinicId()).orElseThrow(() -> new NotFoundException("Клиника не найдена")));
        if (request.specialtyId() != null) template.setSpecialty(specialties.findById(request.specialtyId()).orElseThrow(() -> new NotFoundException("Специальность не найдена")));
        templates.save(template); addVersion(template, request.content(), request.requiredFields(), "Начальная версия", null); return DocumentTemplateResponse.from(template, false, false);
    }

    @Transactional
    public DocumentTemplateVersionResponse version(Long templateId, DocumentTemplateVersionRequest request) {
        DocumentTemplate template = templates.findById(templateId).orElseThrow(() -> new NotFoundException("Шаблон не найден"));
        return DocumentTemplateVersionResponse.from(addVersion(template, request.content(), request.requiredFields(), request.changeDescription(), null));
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateVersionResponse> versions(Long templateId) {
        if (!templates.existsById(templateId)) throw new NotFoundException("Шаблон не найден");
        return versions.findAllByTemplateIdOrderByVersionNumberDesc(templateId).stream().map(DocumentTemplateVersionResponse::from).toList();
    }

    @Transactional
    public void archive(Long templateId, boolean active) { DocumentTemplate template = templates.findById(templateId).orElseThrow(() -> new NotFoundException("Шаблон не найден")); template.setActive(active); templates.save(template); }

    private DocumentTemplateVersion addVersion(DocumentTemplate template, String content, String requiredFields, String description, User author) {
        renderer.render(content, java.util.Map.of());
        int number = versions.findTopByTemplateIdOrderByVersionNumberDesc(template.getId()).map(item -> item.getVersionNumber() + 1).orElse(1);
        DocumentTemplateVersion value = new DocumentTemplateVersion(); value.setTemplate(template); value.setVersionNumber(number); value.setContent(content); value.setRequiredFields(requiredFields); value.setChangeDescription(description); value.setCreatedBy(author); versions.save(value); template.setCurrentVersion(value); templates.save(template); return value;
    }
}
