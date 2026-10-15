package com.qamqormed.service;

import com.qamqormed.dto.DocumentTemplateCreateRequest;
import com.qamqormed.dto.DocumentTemplateResponse;
import com.qamqormed.dto.DocumentTemplateVersionRequest;
import com.qamqormed.dto.DocumentTemplateVersionResponse;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ForbiddenException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class DocumentTemplateService {
    private final DocumentTemplateRepository templates;
    private final DocumentTemplateVersionRepository versions;
    private final DoctorFavoriteTemplateRepository favorites;
    private final DoctorDefaultTemplateRepository defaults;
    private final DoctorProfileRepository doctors;
    private final SpecialtyRepository specialties;
    private final TemplateRenderer renderer;

    public DocumentTemplateService(DocumentTemplateRepository templates, DocumentTemplateVersionRepository versions,
                                   DoctorFavoriteTemplateRepository favorites, DoctorDefaultTemplateRepository defaults,
                                   DoctorProfileRepository doctors, SpecialtyRepository specialties,
                                   TemplateRenderer renderer) {
        this.templates = templates;
        this.versions = versions;
        this.favorites = favorites;
        this.defaults = defaults;
        this.doctors = doctors;
        this.specialties = specialties;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateResponse> visible(Long userId, MedicalDocumentType type, String language) {
        DoctorProfile doctor = doctor(userId);
        TemplateLanguage parsedLanguage = parseLanguage(language);
        List<DocumentTemplate> values = templates.findVisible(doctor.getId(), doctor.getClinic().getId(),
                doctor.getSpecialty().getId(), type, parsedLanguage);
        Set<Long> favoriteIds = favorites.findAllByDoctorId(doctor.getId()).stream()
                .map(item -> item.getTemplate().getId()).collect(Collectors.toSet());
        Set<Long> defaultIds = values.stream()
                .filter(item -> defaults.findByDoctorIdAndDocumentType(doctor.getId(), item.getDocumentType()).map(value -> value.getTemplate().getId().equals(item.getId())).orElse(false))
                .map(DocumentTemplate::getId).collect(Collectors.toSet());
        return values.stream()
                .sorted(Comparator.comparing((DocumentTemplate item) -> !favoriteIds.contains(item.getId()))
                        .thenComparing(item -> !defaultIds.contains(item.getId()))
                        .thenComparing(DocumentTemplate::getName))
                .map(item -> DocumentTemplateResponse.from(item, favoriteIds.contains(item.getId()), defaultIds.contains(item.getId()))).toList();
    }

    @Transactional
    public DocumentTemplateResponse createPersonal(Long userId, DocumentTemplateCreateRequest request) {
        DoctorProfile doctor = doctor(userId);
        DocumentTemplate template = new DocumentTemplate();
        template.setName(request.name().trim());
        template.setDocumentType(request.documentType());
        template.setScope(TemplateScope.PERSONAL);
        template.setOwnerDoctor(doctor);
        template.setSpecialty(request.specialtyId() == null ? doctor.getSpecialty() : specialties.findById(request.specialtyId()).orElseThrow(() -> new NotFoundException("Специальность не найдена")));
        template.setLanguage(request.language());
        templates.save(template);
        if (request.content() != null && !request.content().isBlank()) {
            createVersion(template, doctor.getUser(), request.content(), request.requiredFields(), "Первая версия");
        }
        return DocumentTemplateResponse.from(template, false, false);
    }

    @Transactional
    public DocumentTemplateVersionResponse createVersion(Long userId, Long templateId, DocumentTemplateVersionRequest request) {
        DoctorProfile doctor = doctor(userId);
        DocumentTemplate template = accessible(doctor, templateId);
        if (template.getScope() == TemplateScope.SYSTEM) throw new ForbiddenException("Системный шаблон нельзя изменять");
        return DocumentTemplateVersionResponse.from(createVersion(template, doctor.getUser(), request.content(), request.requiredFields(), request.changeDescription()));
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateVersionResponse> versions(Long userId, Long templateId) {
        DocumentTemplate template = accessible(doctor(userId), templateId);
        return versions.findAllByTemplateIdOrderByVersionNumberDesc(template.getId()).stream().map(DocumentTemplateVersionResponse::from).toList();
    }

    @Transactional
    public void favorite(Long userId, Long templateId, boolean enabled) {
        DoctorProfile doctor = doctor(userId);
        DocumentTemplate template = accessible(doctor, templateId);
        if (enabled && !favorites.existsByDoctorIdAndTemplateId(doctor.getId(), template.getId())) {
            DoctorFavoriteTemplate favorite = new DoctorFavoriteTemplate();
            favorite.setDoctor(doctor); favorite.setTemplate(template); favorites.save(favorite);
        } else if (!enabled) {
            favorites.findByDoctorIdAndTemplateId(doctor.getId(), template.getId()).ifPresent(favorites::delete);
        }
    }

    @Transactional
    public void setDefault(Long userId, MedicalDocumentType type, Long templateId) {
        DoctorProfile doctor = doctor(userId);
        DocumentTemplate template = accessible(doctor, templateId);
        if (template.getDocumentType() != type) throw new BadRequestException("Тип шаблона не совпадает с типом документа");
        DoctorDefaultTemplate value = defaults.findByDoctorIdAndDocumentType(doctor.getId(), type).orElseGet(DoctorDefaultTemplate::new);
        value.setDoctor(doctor); value.setDocumentType(type); value.setTemplate(template); defaults.save(value);
    }

    private DocumentTemplateVersion createVersion(DocumentTemplate template, User author, String content, String requiredFields, String changeDescription) {
        if (content == null || content.isBlank()) throw new BadRequestException("Введите текст шаблона");
        renderer.render(content, java.util.Map.of("document.number", "preview", "document.issueDate", "preview"));
        int next = versions.findTopByTemplateIdOrderByVersionNumberDesc(template.getId()).map(value -> value.getVersionNumber() + 1).orElse(1);
        DocumentTemplateVersion version = new DocumentTemplateVersion();
        version.setTemplate(template); version.setVersionNumber(next); version.setContent(content);
        version.setRequiredFields(requiredFields); version.setCreatedBy(author); version.setChangeDescription(changeDescription);
        versions.save(version);
        template.setCurrentVersion(version); templates.save(template);
        return version;
    }

    private DocumentTemplate accessible(DoctorProfile doctor, Long templateId) {
        DocumentTemplate template = templates.findById(templateId).filter(DocumentTemplate::isActive)
                .orElseThrow(() -> new NotFoundException("Шаблон не найден"));
        boolean allowed = template.getScope() == TemplateScope.SYSTEM
                || (template.getScope() == TemplateScope.PERSONAL && template.getOwnerDoctor() != null && template.getOwnerDoctor().getId().equals(doctor.getId()))
                || (template.getScope() == TemplateScope.CLINIC && template.getClinic() != null && template.getClinic().getId().equals(doctor.getClinic().getId()));
        if (!allowed) throw new ForbiddenException("Нет доступа к этому шаблону");
        return template;
    }

    private DoctorProfile doctor(Long userId) {
        return doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
    }

    private TemplateLanguage parseLanguage(String value) {
        if (value == null || value.isBlank()) return null;
        try { return TemplateLanguage.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new BadRequestException("Неподдерживаемый язык шаблона"); }
    }
}
