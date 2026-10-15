package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.ForbiddenException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read model for the patient medical cabinet. All methods resolve the care
 * recipient first, then query by that recipient's profile/dependent key.
 */
@Service
public class MedicalHistoryService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");

    private final PatientService patients;
    private final MedicalVisitRepository visits;
    private final VisitDiagnosisRepository diagnoses;
    private final PrescriptionRepository prescriptions;
    private final MedicationPrescriptionRepository medications;
    private final LabResultRepository labs;
    private final ReferralRepository referrals;
    private final MedicalCertificateRepository certificates;
    private final MedicalDocumentRepository documents;
    private final ClinicalWorkflowService workflow;

    public MedicalHistoryService(PatientService patients, MedicalVisitRepository visits,
                                 VisitDiagnosisRepository diagnoses, PrescriptionRepository prescriptions,
                                 MedicationPrescriptionRepository medications, LabResultRepository labs,
                                 ReferralRepository referrals, MedicalCertificateRepository certificates,
                                 MedicalDocumentRepository documents, ClinicalWorkflowService workflow) {
        this.patients = patients;
        this.visits = visits;
        this.diagnoses = diagnoses;
        this.prescriptions = prescriptions;
        this.medications = medications;
        this.labs = labs;
        this.referrals = referrals;
        this.certificates = certificates;
        this.documents = documents;
        this.workflow = workflow;
    }

    @Transactional(readOnly = true)
    public MedicalHistoryResponse history(Long userId, Long requestedSubjectId) {
        Scope scope = scope(userId, requestedSubjectId);
        List<MedicalVisit> subjectVisits = visits(scope);
        return new MedicalHistoryResponse(scope.subject(), summary(scope, subjectVisits), timeline(scope, subjectVisits),
                subjectVisits.stream().map(item -> workflow.visitResponse(item, true)).toList());
    }

    @Transactional(readOnly = true)
    public MedicalHistorySummaryResponse summary(Long userId, Long requestedSubjectId) {
        Scope scope = scope(userId, requestedSubjectId);
        return summary(scope, visits(scope));
    }

    @Transactional(readOnly = true)
    public List<MedicalVisitResponse> visitResponses(Long userId, Long requestedSubjectId) {
        return visits(scope(userId, requestedSubjectId)).stream().map(item -> workflow.visitResponse(item, true)).toList();
    }

    @Transactional(readOnly = true)
    public MedicalVisitResponse visit(Long userId, Long requestedSubjectId, Long visitId) {
        MedicalVisit visit = visits(scope(userId, requestedSubjectId)).stream()
                .filter(item -> item.getId().equals(visitId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Посещение не найдено"));
        return workflow.visitResponse(visit, true);
    }

    @Transactional(readOnly = true)
    public List<VisitDiagnosisResponse> diagnosisResponses(Long userId, Long subjectId) {
        return visits(scope(userId, subjectId)).stream()
                .flatMap(visit -> diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream())
                .map(VisitDiagnosisResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> prescriptionResponses(Long userId, Long subjectId) {
        return visits(scope(userId, subjectId)).stream()
                .flatMap(visit -> prescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream())
                .map(PrescriptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationPrescriptionResponse> medicationResponses(Long userId, Long subjectId) {
        return visits(scope(userId, subjectId)).stream()
                .flatMap(visit -> medications.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream())
                .map(MedicationPrescriptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LabResultResponse> labResponses(Long userId, Long subjectId) {
        Scope scope = scope(userId, subjectId);
        return (scope.dependent() == null
                ? labs.findAllByPatientProfileIdOrderByTestDateDesc(scope.profile().getId())
                : labs.findAllByDependentIdOrderByTestDateDesc(scope.dependent().getId()))
                .stream().map(LabResultResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReferralResponse> referralResponses(Long userId, Long subjectId) {
        return visits(scope(userId, subjectId)).stream()
                .flatMap(visit -> referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream())
                .map(ReferralResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalCertificateResponse> certificateResponses(Long userId, Long subjectId) {
        Scope scope = scope(userId, subjectId);
        return (scope.dependent() == null
                ? certificates.findAllByPatientProfileIdOrderByIssuedAtDesc(scope.profile().getId())
                : certificates.findAllByDependentIdOrderByIssuedAtDesc(scope.dependent().getId()))
                .stream().map(MedicalCertificateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalDocumentResponse> documentResponses(Long userId, Long subjectId) {
        return visibleDocuments(scope(userId, subjectId)).stream()
                .map(MedicalDocumentResponse::fromPatient).toList();
    }

    @Transactional(readOnly = true)
    public MedicalDocumentResponse document(Long userId, Long subjectId, Long documentId) {
        return visibleDocuments(scope(userId, subjectId)).stream()
                .filter(item -> item.getId().equals(documentId))
                .map(MedicalDocumentResponse::fromPatient)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Документ не найден"));
    }

    @Transactional(readOnly = true)
    public List<MedicalDocumentResponse> visitDocuments(Long userId, Long subjectId, Long visitId) {
        Scope scope = scope(userId, subjectId);
        MedicalVisit visit = visits.findById(visitId)
                .orElseThrow(() -> new NotFoundException("Посещение не найдено"));
        if (!belongsTo(visit, scope)) throw new NotFoundException("Посещение не найдено");
        return documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(visitId).stream()
                .filter(this::isPatientVisibleDocument)
                .map(MedicalDocumentResponse::fromPatient).toList();
    }

    private Scope scope(Long userId, Long requestedSubjectId) {
        patients.patient(userId);
        PatientProfile profile = patients.profileEntity(userId);
        if (requestedSubjectId == null) {
            return new Scope(MedicalSubjectDto.self(profile), profile, null);
        }
        try {
            Dependent dependent = patients.dependent(userId, requestedSubjectId);
            return new Scope(MedicalSubjectDto.dependent(dependent), profile, dependent);
        } catch (NotFoundException ignored) {
            // Keep compatibility with older clients that sent patientProfile.id.
            if (requestedSubjectId.equals(userId) || profile.getId().equals(requestedSubjectId)) {
                return new Scope(MedicalSubjectDto.self(profile), profile, null);
            }
            if (patients.isPatientUserId(requestedSubjectId)) {
                throw new ForbiddenException("Нет доступа к медицинской карте этого пациента");
            }
            throw new NotFoundException("Пациент не найден");
        }
    }

    private List<MedicalVisit> visits(Scope scope) {
        List<MedicalVisit> result = scope.dependent() == null
                ? visits.findAllByPatientUserIdAndDependentIsNullOrderByCompletedAtDesc(scope.profile().getUser().getId())
                : visits.findAllByDependentIdOrderByCompletedAtDesc(scope.dependent().getId());
        return result.stream().filter(item -> item.getCompletedAt() != null).toList();
    }

    /** Documents become visible after approval, independently of visit completion. */
    private List<MedicalDocument> visibleDocuments(Scope scope) {
        List<MedicalDocument> result = scope.dependent() == null
                ? documents.findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(scope.profile().getUser().getId())
                : documents.findAllByDependentIdOrderByCreatedAtDesc(scope.dependent().getId());
        return result.stream().filter(this::isPatientVisibleDocument).toList();
    }

    private boolean belongsTo(MedicalVisit visit, Scope scope) {
        if (scope.dependent() != null) {
            return visit.getDependent() != null && scope.dependent().getId().equals(visit.getDependent().getId());
        }
        if (visit.getDependent() != null) return false;
        if (visit.getPatientProfile() != null) {
            return visit.getPatientProfile().getUser() != null
                    && scope.profile().getUser().getId().equals(visit.getPatientProfile().getUser().getId());
        }
        return visit.getPatientUser() != null
                && scope.profile().getUser().getId().equals(visit.getPatientUser().getId());
    }

    private MedicalHistorySummaryResponse summary(Scope scope, List<MedicalVisit> subjectVisits) {
        List<VisitDiagnosis> diagnosisItems = subjectVisits.stream().flatMap(item -> diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(item.getId()).stream()).toList();
        List<Prescription> prescriptionItems = subjectVisits.stream().flatMap(item -> prescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(item.getId()).stream()).toList();
        List<MedicationPrescription> medicationItems = subjectVisits.stream().flatMap(item -> medications.findAllByMedicalVisitIdOrderByCreatedAtAsc(item.getId()).stream()).toList();
        List<Referral> referralItems = subjectVisits.stream().flatMap(item -> referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(item.getId()).stream()).toList();
        List<MedicalDocument> documentItems = subjectVisits.stream().flatMap(item -> documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(item.getId()).stream().filter(this::isPatientVisibleDocument)).toList();
        List<LabResult> labItems = scope.dependent() == null
                ? labs.findAllByPatientProfileIdOrderByTestDateDesc(scope.profile().getId())
                : labs.findAllByDependentIdOrderByTestDateDesc(scope.dependent().getId());
        List<MedicalCertificate> certificateItems = scope.dependent() == null
                ? certificates.findAllByPatientProfileIdOrderByIssuedAtDesc(scope.profile().getId())
                : certificates.findAllByDependentIdOrderByIssuedAtDesc(scope.dependent().getId());
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        return new MedicalHistorySummaryResponse(subjectVisits.size(), diagnosisItems.size(),
                prescriptionItems.stream().filter(item -> item.getEndDate() == null || !item.getEndDate().isBefore(today)).count(),
                medicationItems.stream().filter(item -> item.getEndDate() == null || !item.getEndDate().isBefore(today)).count(),
                labItems.stream().filter(item -> !"COMPLETED".equalsIgnoreCase(item.getStatus()) && !"CANCELLED".equalsIgnoreCase(item.getStatus())).count(),
                referralItems.stream().filter(item -> item.getStatus() == ReferralStatus.CREATED || item.getStatus() == ReferralStatus.ACTIVE).count(),
                certificateItems.size(), documentItems.size());
    }

    private List<MedicalTimelineItemResponse> timeline(Scope scope, List<MedicalVisit> subjectVisits) {
        List<MedicalTimelineItemResponse> result = new ArrayList<>();
        for (MedicalVisit visit : subjectVisits) {
            Instant visitDate = visit.getCompletedAt() == null ? visit.getCreatedAt() : toInstant(visit.getCompletedAt());
            result.add(new MedicalTimelineItemResponse("VISIT", visitDate,
                    "Приём " + (visit.getAppointment().getDoctor().getSpecialty() == null ? "врача" : visit.getAppointment().getDoctor().getSpecialty().getName()),
                    firstNonBlank(visit.getChiefComplaint(), visit.getDiagnosis(), "Завершённый приём"), visit.getId(), visit.getId()));
            diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("DIAGNOSIS", item.getCreatedAt(), item.getIcd10Code().getCode(), item.getIcd10Code().getName(), item.getId(), visit.getId())));
            prescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("PRESCRIPTION", item.getCreatedAt(), item.getTitle(), item.getDescription(), item.getId(), visit.getId())));
            medications.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("MEDICATION", item.getCreatedAt(), item.getMedicationName(), item.getDosage(), item.getId(), visit.getId())));
            labs.findAllByMedicalVisitIdOrderByTestDateDesc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("LAB_RESULT", dateOf(item.getTestDate(), item.getCreatedAt()), item.getTestName(), item.getResult(), item.getId(), visit.getId())));
            referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("REFERRAL", item.getCreatedAt(), item.getReferralType().name(), item.getReason(), item.getId(), visit.getId())));
            certificates.findAllByMedicalVisitIdOrderByIssuedAtDesc(visit.getId()).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("CERTIFICATE", item.getIssuedAt(), item.getType(), item.getDescription(), item.getId(), visit.getId())));
            documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().filter(this::isPatientVisibleDocument).forEach(item ->
                    result.add(new MedicalTimelineItemResponse("DOCUMENT", item.getCreatedAt(), item.getTitle(), item.getDescription(), item.getId(), visit.getId())));
        }
        return result.stream().sorted(Comparator.comparing(MedicalTimelineItemResponse::date, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    private Instant dateOf(LocalDate date, Instant fallback) {
        return date == null ? fallback : date.atStartOfDay(CLINIC_ZONE).toInstant();
    }

    private Instant toInstant(LocalDateTime value) { return value.atZone(CLINIC_ZONE).toInstant(); }

    private String firstNonBlank(String first, String second, String fallback) {
        return first != null && !first.isBlank() ? first : second != null && !second.isBlank() ? second : fallback;
    }

    private boolean isPatientVisibleDocument(MedicalDocument document) {
        return document.getStatus() == null || document.getStatus() == MedicalDocumentStatus.APPROVED;
    }

    private record Scope(MedicalSubjectDto subject, PatientProfile profile, Dependent dependent) {}
}
