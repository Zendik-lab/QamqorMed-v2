package com.qamqormed.service;

import com.qamqormed.dto.DocumentContentRequest;
import com.qamqormed.dto.DocumentDraftRequest;
import com.qamqormed.dto.MedicalDocumentResponse;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ForbiddenException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MedicalDocumentService {
    private static final Pattern REQUIRED_MARKER = Pattern.compile("\\[ТРЕБУЕТ ЗАПОЛНЕНИЯ: ([^]]+)]");
    private final MedicalDocumentRepository documents;
    private final DocumentTemplateRepository templates;
    private final DocumentAuditLogRepository audits;
    private final AppointmentRepository appointments;
    private final MedicalVisitRepository visits;
    private final DoctorProfileRepository doctors;
    private final VisitDiagnosisRepository diagnoses;
    private final TreatmentRepository treatments;
    private final ReferralRepository referrals;
    private final NurseIntakeRepository nurseIntakes;
    private final PatientService patients;
    private final PdfDocumentService pdf;
    private final TemplateRenderer renderer;
    private final NotificationService notifications;

    public MedicalDocumentService(MedicalDocumentRepository documents, DocumentTemplateRepository templates,
                                  DocumentAuditLogRepository audits, AppointmentRepository appointments,
                                  MedicalVisitRepository visits, DoctorProfileRepository doctors,
                                  VisitDiagnosisRepository diagnoses, TreatmentRepository treatments,
                                  ReferralRepository referrals, NurseIntakeRepository nurseIntakes, PatientService patients,
                                  PdfDocumentService pdf, TemplateRenderer renderer, NotificationService notifications) {
        this.documents = documents; this.templates = templates; this.audits = audits;
        this.appointments = appointments; this.visits = visits; this.doctors = doctors;
        this.diagnoses = diagnoses; this.treatments = treatments; this.referrals = referrals; this.nurseIntakes = nurseIntakes;
        this.patients = patients; this.pdf = pdf; this.renderer = renderer; this.notifications = notifications;
    }

    @Transactional
    public MedicalDocumentResponse createDraft(Long userId, Long appointmentId, DocumentDraftRequest request) {
        DoctorProfile doctor = doctor(userId);
        Appointment appointment = appointments.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        if (appointment.getCanonicalStatus() != AppointmentStatus.IN_APPOINTMENT) {
            throw new BadRequestException("Сначала начните медицинский приём");
        }
        MedicalVisit visit = visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
        DocumentTemplate template = templates.findById(request.templateId()).filter(DocumentTemplate::isActive)
                .orElseThrow(() -> new NotFoundException("Шаблон не найден"));
        if (!visibleTo(template, doctor)) throw new ForbiddenException("Нет доступа к этому шаблону");
        if (template.getCurrentVersion() == null) throw new BadRequestException("У шаблона нет опубликованной версии");
        MedicalDocument document = new MedicalDocument();
        document.setMedicalVisit(visit); document.setPatientUser(documentOwner(visit)); document.setDependent(visit.getDependent());
        document.setDoctorUser(visit.getDoctorUser()); document.setType(template.getDocumentType()); document.setTitle(template.getName());
        document.setStatus(MedicalDocumentStatus.DRAFT); document.setLanguage(template.getLanguage().name());
        document.setTemplate(template); document.setTemplateVersion(template.getCurrentVersion());
        TemplateRenderer.Rendered rendered = renderer.render(template.getCurrentVersion().getContent(), context(visit, null));
        document.setDraftContent(rendered.content());
        document.setDescription("Черновик создан на основе шаблона «" + template.getName() + "»");
        document = documents.save(document);
        audit(document, doctor.getUser(), "CREATED", "templateVersion=" + template.getCurrentVersion().getVersionNumber());
        return MedicalDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public MedicalDocumentResponse doctorDocument(Long userId, Long documentId) {
        return MedicalDocumentResponse.from(doctorDocumentEntity(userId, documentId));
    }

    @Transactional
    public MedicalDocumentResponse saveDraft(Long userId, Long documentId, DocumentContentRequest request) {
        MedicalDocument document = doctorDocumentEntity(userId, documentId);
        if (document.getStatus() != MedicalDocumentStatus.DRAFT) throw new BadRequestException("Утверждённый документ нельзя редактировать");
        renderer.render(request.content(), Map.of());
        document.setDraftContent(request.content());
        document = documents.save(document);
        audit(document, doctor(userId).getUser(), "DRAFT_SAVED", null);
        return MedicalDocumentResponse.from(document);
    }

    @Transactional
    public MedicalDocumentResponse approve(Long userId, Long documentId) {
        MedicalDocument document = doctorDocumentEntity(userId, documentId);
        if (document.getStatus() != MedicalDocumentStatus.DRAFT) throw new BadRequestException("Документ уже утверждён или аннулирован");
        String content = document.getDraftContent();
        if (content == null || content.isBlank()) throw new BadRequestException("Заполните содержимое документа");
        TemplateRenderer.Rendered rendered = renderer.render(content, context(document.getMedicalVisit(), document));
        if (!rendered.missingFields().isEmpty()) throw new BadRequestException("Заполните обязательные поля: " + String.join(", ", rendered.missingFields()));
        var markers = REQUIRED_MARKER.matcher(content);
        if (markers.find()) throw new BadRequestException("В документе остались незаполненные поля");
        document.setFinalizedContent(content);
        document.setStatus(MedicalDocumentStatus.APPROVED);
        document.setApprovedAt(java.time.Instant.now());
        document = documents.save(document);
        audit(document, doctor(userId).getUser(), "APPROVED", null);
        notifications.medical(document.getPatientUser(), NotificationType.DOCUMENT_CREATED,
                "Документ готов", "Врач добавил новый медицинский документ.", document.getMedicalVisit().getAppointment(),
                "/patient/documents", "document-approved-" + document.getId());
        return MedicalDocumentResponse.from(document);
    }

    @Transactional
    public MedicalDocumentResponse cancel(Long userId, Long documentId) {
        MedicalDocument document = doctorDocumentEntity(userId, documentId);
        if (document.getStatus() == MedicalDocumentStatus.CANCELLED) return MedicalDocumentResponse.from(document);
        document.setStatus(MedicalDocumentStatus.CANCELLED); document.setCancelledAt(java.time.Instant.now());
        document = documents.save(document); audit(document, doctor(userId).getUser(), "CANCELLED", null);
        return MedicalDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public byte[] doctorPdf(Long userId, Long documentId) { return pdf.render(doctorDocumentEntity(userId, documentId)); }

    @Transactional(readOnly = true)
    public byte[] patientPdf(Long userId, Long subjectId, Long documentId) {
        MedicalDocument document = documents.findById(documentId).orElseThrow(() -> new NotFoundException("Документ не найден"));
        if (!isPatientVisible(document) || !patientCanAccess(userId, subjectId, document)) {
            throw new ForbiddenException("Документ недоступен");
        }
        return pdf.render(document);
    }

    @Transactional(readOnly = true)
    public List<MedicalDocument> approvedForVisit(Long visitId) {
        return documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(visitId).stream()
                .filter(item -> item.getStatus() == MedicalDocumentStatus.APPROVED).toList();
    }

    private MedicalDocument doctorDocumentEntity(Long userId, Long documentId) {
        return documents.findByIdAndDoctorUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Документ не найден"));
    }

    private DoctorProfile doctor(Long userId) {
        return doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
    }

    private boolean visibleTo(DocumentTemplate template, DoctorProfile doctor) {
        return template.getScope() == TemplateScope.SYSTEM
                || (template.getScope() == TemplateScope.PERSONAL && template.getOwnerDoctor() != null && template.getOwnerDoctor().getId().equals(doctor.getId()))
                || (template.getScope() == TemplateScope.CLINIC && template.getClinic() != null && template.getClinic().getId().equals(doctor.getClinic().getId()));
    }

    private Map<String, String> context(MedicalVisit visit, MedicalDocument document) {
        Map<String, String> values = new LinkedHashMap<>();
        User patient = visit.getPatientUser();
        String patientName = visit.getDependent() == null
                ? fullName(visit.getPatientProfile() == null ? patient.getLastName() : visit.getPatientProfile().getLastName(), visit.getPatientProfile() == null ? patient.getFirstName() : visit.getPatientProfile().getFirstName(), visit.getPatientProfile() == null ? patient.getMiddleName() : visit.getPatientProfile().getMiddleName())
                : fullName(visit.getDependent().getLastName(), visit.getDependent().getFirstName(), visit.getDependent().getMiddleName());
        LocalDate birthDate = visit.getDependent() == null
                ? visit.getPatientProfile() == null ? patient.getDateOfBirth() : visit.getPatientProfile().getBirthDate()
                : visit.getDependent().getDateOfBirth();
        String iin = visit.getDependent() == null
                ? visit.getPatientProfile() == null ? patient.getIin() : visit.getPatientProfile().getIin()
                : visit.getDependent().getIin();
        User doctorUser = visit.getDoctorUser();
        values.put("patient.fullName", patientName); values.put("patient.birthDate", birthDate == null ? null : birthDate.toString()); values.put("patient.iin", iin);
        values.put("doctor.fullName", fullName(doctorUser.getLastName(), doctorUser.getFirstName(), doctorUser.getMiddleName()));
        values.put("doctor.specialty", visit.getDoctorProfile() == null || visit.getDoctorProfile().getSpecialty() == null ? null : visit.getDoctorProfile().getSpecialty().getName());
        values.put("clinic.name", visit.getClinic() == null ? null : visit.getClinic().getName()); values.put("clinic.address", visit.getClinic() == null ? null : visit.getClinic().getAddress());
        values.put("appointment.date", visit.getAppointment().getAppointmentAt() == null ? null : visit.getAppointment().getAppointmentAt().toLocalDate().toString());
        values.put("appointment.time", visit.getAppointment().getAppointmentAt() == null ? null : visit.getAppointment().getAppointmentAt().toLocalTime().toString());
        var primary = diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream().findFirst().orElse(null);
        values.put("diagnosis.code", primary == null ? visit.getDiagnosisCode() : (primary.getIcd10CodeSnapshot() == null ? primary.getIcd10Code().getCode() : primary.getIcd10CodeSnapshot()));
        values.put("diagnosis.name", primary == null ? visit.getDiagnosisName() : (primary.getIcd10NameSnapshot() == null ? primary.getIcd10Code().getName() : primary.getIcd10NameSnapshot()));
        values.put("diagnosis.secondary", diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream()
                .filter(item -> !item.isPrimaryDiagnosis())
                .map(item -> (item.getIcd10CodeSnapshot() == null ? item.getIcd10Code().getCode() : item.getIcd10CodeSnapshot()) + " — " + (item.getIcd10NameSnapshot() == null ? item.getIcd10Code().getName() : item.getIcd10NameSnapshot()))
                .reduce((left, right) -> left + "; " + right).orElse(null));
        values.put("prescriptions.summary", treatments.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .map(item -> java.util.stream.Stream.of(item.getName(), item.getDosage(), item.getFrequency(), item.getDuration(), item.getInstructions())
                        .filter(value -> value != null && !value.isBlank()).reduce((left, right) -> left + "; " + right).orElse(""))
                .filter(value -> !value.isBlank()).reduce((left, right) -> left + "\n" + right).orElse(null));
        values.put("referrals.summary", referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .map(item -> item.getReferralType().name() + (item.getReason() == null || item.getReason().isBlank() ? "" : " — " + item.getReason()))
                .reduce((left, right) -> left + "\n" + right).orElse(null));
        values.put("visit.nurseAssessment", nurseIntakes.findByAppointmentId(visit.getAppointment().getId()).map(this::nurseAssessment).orElse(null));
        values.put("visit.complaints", visit.getChiefComplaint()); values.put("visit.anamnesis", visit.getAnamnesis()); values.put("visit.examination", visit.getDoctorNotes()); values.put("visit.recommendations", visit.getRecommendations());
        values.put("document.number", document == null ? "PREVIEW" : document.getDocumentNumber()); values.put("document.issueDate", document == null ? LocalDate.now().toString() : document.getCreatedAt() == null ? LocalDate.now().toString() : document.getCreatedAt().toString());
        return values;
    }

    private boolean patientCanAccess(Long userId, Long subjectId, MedicalDocument document) {
        boolean dependent = document.getDependent() != null;
        if (dependent) {
            if (!patients.isOwnedDependentId(userId, document.getDependent().getId())) return false;
            return subjectId == null || subjectId.equals(document.getDependent().getId());
        }
        User subjectUser = document.getMedicalVisit().getPatientProfile() != null
                && document.getMedicalVisit().getPatientProfile().getUser() != null
                ? document.getMedicalVisit().getPatientProfile().getUser()
                : document.getPatientUser();
        return subjectUser != null && subjectUser.getId().equals(userId)
                && (subjectId == null || subjectId.equals(userId) || patients.profileEntity(userId).getId().equals(subjectId));
    }

    private boolean isPatientVisible(MedicalDocument document) {
        return document.getStatus() == null || document.getStatus() == MedicalDocumentStatus.APPROVED;
    }

    private User documentOwner(MedicalVisit visit) {
        if (visit.getDependent() != null && visit.getDependent().getOwner() != null) return visit.getDependent().getOwner();
        if (visit.getPatientProfile() != null && visit.getPatientProfile().getUser() != null) return visit.getPatientProfile().getUser();
        return visit.getPatientUser();
    }

    private String nurseAssessment(NurseIntake item) {
        return java.util.stream.Stream.of(item.getComplaintSummary(), item.getBloodPressure(),
                        item.getTemperature() == null ? null : "Температура: " + item.getTemperature(),
                        item.getPulse() == null ? null : "Пульс: " + item.getPulse(),
                        item.getOxygenSaturation() == null ? null : "SpO2: " + item.getOxygenSaturation(),
                        item.getHeight() == null ? null : "Рост: " + item.getHeight(),
                        item.getWeight() == null ? null : "Вес: " + item.getWeight(), item.getNurseNotes())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + "; " + right).orElse("");
    }

    private void audit(MedicalDocument document, User actor, String action, String metadata) {
        DocumentAuditLog audit = new DocumentAuditLog(); audit.setDocument(document); audit.setActor(actor); audit.setAction(action); audit.setMetadata(metadata); audits.save(audit);
    }

    private String fullName(String last, String first, String middle) { return String.join(" ", last == null ? "" : last, first == null ? "" : first, middle == null ? "" : middle).trim().replaceAll(" +", " "); }
}
