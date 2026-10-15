package com.qamqormed.service;

import com.qamqormed.dto.AppointmentDetailsResponse;
import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.DoctorPatientDetailResponse;
import com.qamqormed.dto.DoctorPatientDirectoryResponse;
import com.qamqormed.dto.DoctorPatientSummaryResponse;
import com.qamqormed.dto.DoctorReferralResponse;
import com.qamqormed.dto.MedicalDocumentRequest;
import com.qamqormed.dto.MedicalDocumentResponse;
import com.qamqormed.dto.LabResultResponse;
import com.qamqormed.dto.MedicalCertificateResponse;
import com.qamqormed.dto.MedicalRecordCreateRequest;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.dto.MedicalVisitResponse;
import com.qamqormed.dto.MedicalVisitUpsertRequest;
import com.qamqormed.dto.NurseIntakeRequest;
import com.qamqormed.dto.NurseIntakeResponse;
import com.qamqormed.dto.ReferralRequest;
import com.qamqormed.dto.ReferralResponse;
import com.qamqormed.dto.TreatmentRequest;
import com.qamqormed.dto.TreatmentResponse;
import com.qamqormed.entity.Appointment;
import com.qamqormed.entity.AppointmentStatus;
import com.qamqormed.entity.Clinic;
import com.qamqormed.entity.DoctorNurseAssignment;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.MedicalDocument;
import com.qamqormed.entity.MedicalDocumentStatus;
import com.qamqormed.entity.MedicalRecord;
import com.qamqormed.entity.MedicalVisit;
import com.qamqormed.entity.NurseIntake;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.entity.Referral;
import com.qamqormed.entity.ReferralStatus;
import com.qamqormed.entity.ReferralType;
import com.qamqormed.entity.Treatment;
import com.qamqormed.entity.User;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.ClinicRepository;
import com.qamqormed.repository.DoctorNurseAssignmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.MedicalDocumentRepository;
import com.qamqormed.repository.LabResultRepository;
import com.qamqormed.repository.MedicalCertificateRepository;
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.MedicalVisitRepository;
import com.qamqormed.repository.NurseIntakeRepository;
import com.qamqormed.repository.NurseProfileRepository;
import com.qamqormed.repository.ReferralRepository;
import com.qamqormed.repository.SpecialtyRepository;
import com.qamqormed.repository.TreatmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ClinicalWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(ClinicalWorkflowService.class);
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final NurseIntakeRepository intakes;
    private final MedicalVisitRepository visits;
    private final TreatmentRepository treatments;
    private final ReferralRepository referrals;
    private final MedicalDocumentRepository documents;
    private final SpecialtyRepository specialties;
    private final ClinicRepository clinics;
    private final PatientService patients;
    private final com.qamqormed.repository.VisitDiagnosisRepository visitDiagnoses;
    private final com.qamqormed.repository.PrescriptionRepository structuredPrescriptions;
    private final com.qamqormed.repository.MedicationPrescriptionRepository medicationPrescriptions;
    private final com.qamqormed.repository.ICD10CodeRepository icd10Codes;
    private final LabResultRepository labResults;
    private final MedicalCertificateRepository certificates;
    private final NotificationService notifications;
    private final AppointmentStatusHistoryService statusHistory;

    public ClinicalWorkflowService(DoctorProfileRepository doctors, NurseProfileRepository nurses,
                                   DoctorNurseAssignmentRepository assignments, AppointmentRepository appointments,
                                   MedicalRecordRepository records, NurseIntakeRepository intakes,
                                   MedicalVisitRepository visits, TreatmentRepository treatments,
                                   ReferralRepository referrals, MedicalDocumentRepository documents,
                                   SpecialtyRepository specialties, ClinicRepository clinics, PatientService patients,
                                   com.qamqormed.repository.VisitDiagnosisRepository visitDiagnoses,
                                   com.qamqormed.repository.PrescriptionRepository structuredPrescriptions,
                                   com.qamqormed.repository.MedicationPrescriptionRepository medicationPrescriptions,
                                   com.qamqormed.repository.ICD10CodeRepository icd10Codes,
                                   LabResultRepository labResults, MedicalCertificateRepository certificates,
                                   NotificationService notifications, AppointmentStatusHistoryService statusHistory) {
        this.doctors = doctors;
        this.nurses = nurses;
        this.assignments = assignments;
        this.appointments = appointments;
        this.records = records;
        this.intakes = intakes;
        this.visits = visits;
        this.treatments = treatments;
        this.referrals = referrals;
        this.documents = documents;
        this.specialties = specialties;
        this.clinics = clinics;
        this.patients = patients;
        this.visitDiagnoses = visitDiagnoses;
        this.structuredPrescriptions = structuredPrescriptions;
        this.medicationPrescriptions = medicationPrescriptions;
        this.icd10Codes = icd10Codes;
        this.labResults = labResults;
        this.certificates = certificates;
        this.notifications = notifications;
        this.statusHistory = statusHistory;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> doctorAppointments(Long userId) {
        return doctorAppointments(userId, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> doctorAppointments(Long userId, LocalDate date, AppointmentStatus status, String query) {
        DoctorProfile doctor = doctorForUser(userId);
        return appointments.findAllByDoctorIdOrderByAppointmentAtAsc(doctor.getId()).stream()
                .filter(appointment -> matches(appointment, date, status, query))
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDetailsResponse doctorAppointmentDetails(Long userId, Long appointmentId) {
        return details(doctorAppointmentEntity(userId, appointmentId));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse doctorAppointment(Long userId, Long appointmentId) {
        return AppointmentResponse.from(doctorAppointmentEntity(userId, appointmentId));
    }

    /**
     * Builds the doctor's patient catalogue from that doctor's appointments only.
     * The authenticated user is resolved to a DoctorProfile inside this service;
     * no doctor id supplied by the client participates in the scope decision.
     */
    @Transactional(readOnly = true)
    public DoctorPatientDirectoryResponse doctorPatientDirectory(Long userId, String query, String status,
                                                                  LocalDate date, String period, String type, Long clinicId) {
        DoctorProfile doctor = doctorForUser(userId);
        AppointmentStatus requestedStatus = parseStatus(status);
        List<Appointment> scoped = appointments.findAllByDoctorIdOrderByAppointmentAtAsc(doctor.getId()).stream()
                .filter(appointment -> matchesPatientFilter(appointment, query, requestedStatus, date, period, type, clinicId))
                .toList();
        List<AppointmentDetailsResponse> details = scoped.stream().map(this::details).toList();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentDetailsResponse> todayItems = details.stream()
                .filter(item -> item.appointmentAt().toLocalDate().equals(today))
                .toList();
        List<AppointmentDetailsResponse> upcoming = details.stream()
                .filter(item -> item.appointmentAt().isAfter(now))
                .toList();
        List<AppointmentDetailsResponse> history = details.stream()
                .filter(item -> item.appointmentAt().isBefore(now)
                        && "COMPLETED".equals(item.status()))
                .sorted(Comparator.comparing(AppointmentDetailsResponse::appointmentAt).reversed())
                .toList();

        Map<String, List<Appointment>> bySubject = new LinkedHashMap<>();
        scoped.forEach(appointment -> bySubject.computeIfAbsent(subjectKey(appointment), ignored -> new ArrayList<>()).add(appointment));
        List<DoctorPatientSummaryResponse> patients = bySubject.values().stream()
                .map(this::patientSummary)
                .sorted(Comparator.comparing(DoctorPatientSummaryResponse::lastAppointmentDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return new DoctorPatientDirectoryResponse(todayItems, upcoming, history, patients);
    }

    /**
     * Resolves a patient/dependent only through appointments owned by the current
     * doctor's profile. A patient without a relationship to this doctor is never
     * loaded and is returned as the same safe 404 as an unknown patient.
     */
    @Transactional(readOnly = true)
    public DoctorPatientDetailResponse doctorPatientDetail(Long userId, Long patientId, String type) {
        DoctorProfile doctor = doctorForUser(userId);
        List<Appointment> scoped = appointments.findAllByDoctorIdOrderByAppointmentAtAsc(doctor.getId()).stream()
                .filter(appointment -> subjectMatches(appointment, patientId, type))
                .sorted(Comparator.comparing(Appointment::getAppointmentAt).reversed())
                .toList();
        if (scoped.isEmpty()) throw new NotFoundException("Пациент не найден");

        List<AppointmentDetailsResponse> details = scoped.stream().map(this::details).toList();
        List<com.qamqormed.dto.MedicalVisitResponse> visits = details.stream()
                .map(AppointmentDetailsResponse::medicalVisit)
                .filter(Objects::nonNull)
                .filter(visit -> visit.completedAt() != null)
                .sorted(Comparator.comparing(com.qamqormed.dto.MedicalVisitResponse::appointmentAt).reversed())
                .toList();
        List<LabResultResponse> labResults = visits.stream()
                .flatMap(visit -> this.labResults.findAllByMedicalVisitIdOrderByTestDateDesc(visit.id()).stream())
                .map(LabResultResponse::from)
                .toList();
        List<MedicalCertificateResponse> certificates = visits.stream()
                .flatMap(visit -> this.certificates.findAllByMedicalVisitIdOrderByIssuedAtDesc(visit.id()).stream())
                .map(MedicalCertificateResponse::from)
                .toList();
        AppointmentDetailsResponse current = details.stream()
                .filter(item -> item.appointmentAt().toLocalDate().equals(LocalDate.now()))
                .filter(item -> !Set.of("COMPLETED", "CANCELLED", "NO_SHOW").contains(item.status()))
                .min(Comparator.comparing(AppointmentDetailsResponse::appointmentAt))
                .orElse(null);
        return new DoctorPatientDetailResponse(patientSummary(scoped), current, details, visits, labResults, certificates);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> doctorSubjectHistory(Long userId, Long appointmentId) {
        return subjectHistory(doctorAppointmentEntity(userId, appointmentId));
    }

    @Transactional
    public AppointmentDetailsResponse startDoctorVisit(Long userId, Long appointmentId) {
        DoctorProfile doctor = doctorForUser(userId);
        Appointment appointment = appointments.findByIdAndDoctorIdForUpdate(appointmentId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        AppointmentStatus current = canonical(appointment.getStatus());
        log.info("Starting medical visit: appointmentId={}, doctorId={}, patientUserId={}, patientProfileId={}, dependentId={}, status={}",
                appointmentId, doctor.getId(), appointment.getBookedBy().getId(),
                appointment.getPatientProfile() == null ? null : appointment.getPatientProfile().getId(),
                appointment.getDependent() == null ? null : appointment.getDependent().getId(), current);

        // A retry caused by a double click or a lost response must not create a
        // second visit. Return the same in-progress visit after making sure its
        // start timestamp exists.
        if (current == AppointmentStatus.IN_APPOINTMENT) {
            MedicalVisit existing = visitFor(appointment, doctor.getUser());
            if (existing.getStartedAt() == null) existing.setStartedAt(LocalDateTime.now());
            visits.save(existing);
            return details(appointment);
        }
        requireStatus(appointment, AppointmentStatus.READY);
        MedicalVisit visit = visitFor(appointment, appointment.getDoctor().getUser());
        if (visit.getStartedAt() == null) visit.setStartedAt(LocalDateTime.now());
        visits.save(visit);
        appointment.setStatus(AppointmentStatus.IN_APPOINTMENT);
        appointment.setStartedAt(java.time.Instant.now());
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional
    public MedicalVisitResponse saveMedicalVisit(Long userId, Long appointmentId, MedicalVisitUpsertRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.IN_APPOINTMENT);
        MedicalVisit visit = visitFor(appointment, appointment.getDoctor().getUser());
        applyVisit(visit, request);
        return visitResponse(visits.save(visit));
    }

    /** Compatibility endpoint for the first workflow version. */
    @Transactional
    public MedicalRecordResponse saveMedicalRecord(Long userId, Long appointmentId, MedicalRecordCreateRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        if (isBlank(request.diagnosis())) throw new BadRequestException("Перед завершением укажите диагноз");
        if (canonical(appointment.getStatus()) == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Отменённая запись недоступна");
        }
        if (canonical(appointment.getStatus()) == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Приём уже завершён");
        }
        MedicalVisit visit = visitFor(appointment, appointment.getDoctor().getUser());
        visit.setDiagnosis(trimToNull(request.diagnosis()));
        visit.setRecommendations(trimToNull(request.recommendations()));
        visit.setStartedAt(visit.getStartedAt() == null ? LocalDateTime.now() : visit.getStartedAt());
        visit.setCompletedAt(LocalDateTime.now());
        if (!isBlank(request.prescriptions())) {
            Treatment treatment = new Treatment();
            applyOwnership(treatment, visit);
            treatment.setName(trimToNull(request.prescriptions()));
            treatments.save(treatment);
        }
        if (!isBlank(request.referrals())) {
            Referral referral = new Referral();
            applyOwnership(referral, visit);
            referral.setReferralType(ReferralType.OTHER);
            referral.setReason(trimToNull(request.referrals()));
            referral.setStatus(ReferralStatus.CREATED);
            referrals.save(referral);
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointments.save(appointment);
        visit = visits.save(visit);
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.VISIT_COMPLETED,
                "Результаты приёма готовы", "Откройте медицинскую историю, чтобы посмотреть результат.", appointment,
                "/patient/medical-history", "visit-completed-" + appointment.getId());
        return MedicalRecordResponse.from(syncLegacyRecord(visit));
    }

    @Transactional
    public AppointmentResponse updateDoctorStatus(Long userId, Long appointmentId, AppointmentStatusRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        AppointmentStatus target = canonical(request.status());
        if (target != AppointmentStatus.IN_APPOINTMENT && target != AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Врач может начать или отменить только свой приём");
        }
        if (target == AppointmentStatus.IN_APPOINTMENT) {
            startDoctorVisit(userId, appointmentId);
        } else {
            transition(appointment, target);
            appointments.save(appointment);
        }
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse updateNurseStatus(Long userId, Long appointmentId, AppointmentStatusRequest request) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus target = canonical(request.status());
        if (target != AppointmentStatus.ARRIVED && target != AppointmentStatus.PREPARING && target != AppointmentStatus.READY && target != AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Медсестра может изменить только этап подготовки пациента");
        }
        transition(appointment, target);
        applyStatusTimestamp(appointment, target);
        appointments.save(appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentDetailsResponse completeDoctorVisit(Long userId, Long appointmentId) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.IN_APPOINTMENT);
        MedicalVisit visit = visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
        if (isBlank(visit.getDiagnosis())) throw new BadRequestException("Перед завершением укажите диагноз");
        syncStructuredDiagnosis(visit);
        visit.setCompletedAt(LocalDateTime.now());
        visits.save(visit);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(java.time.Instant.now());
        appointments.save(appointment);
        syncLegacyRecord(visit);
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.VISIT_COMPLETED,
                "Результаты приёма готовы", "Откройте медицинскую историю, чтобы посмотреть результат.", appointment,
                "/patient/medical-history", "visit-completed-" + appointment.getId());
        return details(appointment);
    }

    @Transactional
    public TreatmentResponse addTreatment(Long userId, Long appointmentId, TreatmentRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.IN_APPOINTMENT);
        if (isBlank(request.name())) throw new BadRequestException("Укажите название препарата или назначения");
        MedicalVisit visit = requiredVisit(appointmentId);
        Treatment treatment = new Treatment();
        applyOwnership(treatment, visit);
        treatment.setName(trimToNull(request.name()));
        treatment.setDosage(trimToNull(request.dosage()));
        treatment.setFrequency(trimToNull(request.frequency()));
        treatment.setDuration(trimToNull(request.duration()));
        treatment.setInstructions(trimToNull(request.instructions()));
        Treatment saved = treatments.save(treatment);
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.PRESCRIPTION_CREATED,
                "Добавлено новое назначение", "Врач добавил новое назначение.", appointment, "/patient/documents", "treatment-created-" + saved.getId());
        return TreatmentResponse.from(saved);
    }

    @Transactional
    public ReferralResponse addReferral(Long userId, Long appointmentId, ReferralRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.IN_APPOINTMENT);
        if (request.referralType() == null) throw new BadRequestException("Укажите тип направления");
        MedicalVisit visit = requiredVisit(appointmentId);
        Referral referral = new Referral();
        applyOwnership(referral, visit);
        referral.setReferralType(request.referralType());
        referral.setTargetSpecialty(request.targetSpecialtyId() == null ? null : specialties.findById(request.targetSpecialtyId())
                .orElseThrow(() -> new NotFoundException("Специальность направления не найдена")));
        referral.setTargetClinic(request.targetClinicId() == null ? null : clinics.findById(request.targetClinicId())
                .orElseThrow(() -> new NotFoundException("Клиника направления не найдена")));
        referral.setTargetDoctor(request.targetDoctorId() == null ? null : doctors.findById(request.targetDoctorId())
                .orElseThrow(() -> new NotFoundException("Врач направления не найден")));
        referral.setReason(trimToNull(request.reason()));
        referral.setNotes(trimToNull(request.notes()));
        referral.setValidUntil(request.validUntil());
        referral.setStatus(ReferralStatus.CREATED);
        Referral saved = referrals.save(referral);
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.REFERRAL_CREATED,
                "Добавлено новое направление", "Врач добавил новое направление.", appointment, "/patient/documents", "referral-created-" + saved.getId());
        return ReferralResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorReferralResponse> doctorReferrals(Long userId, String query, ReferralStatus status,
                                                        ReferralType type, LocalDate from, LocalDate to) {
        doctorForUser(userId);
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return referrals.findAllByDoctorUserIdOrderByIssuedAtDesc(userId).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> type == null || item.getReferralType() == type)
                .filter(item -> dateInRange(item.getIssuedAt(), from, to))
                .filter(item -> normalized.isBlank() || subjectName(item.getMedicalVisit().getAppointment()).toLowerCase(Locale.ROOT).contains(normalized)
                        || valueOf(subjectIin(item.getMedicalVisit().getAppointment())).contains(normalized))
                .map(DoctorReferralResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] doctorReferralPdf(Long userId, Long referralId, ReferralPdfService pdf) {
        doctorForUser(userId);
        Referral referral = referrals.findByIdAndDoctorUserId(referralId, userId)
                .orElseThrow(() -> new NotFoundException("Направление не найдено"));
        return pdf.render(referral);
    }

    @Transactional
    public MedicalDocumentResponse addDocument(Long userId, Long appointmentId, MedicalDocumentRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.IN_APPOINTMENT);
        if (request.type() == null || isBlank(request.title())) throw new BadRequestException("Укажите тип и название документа");
        MedicalVisit visit = requiredVisit(appointmentId);
        MedicalDocument document = new MedicalDocument();
        document.setMedicalVisit(visit);
        document.setPatientUser(documentOwner(visit));
        document.setDependent(visit.getDependent());
        document.setDoctorUser(visit.getDoctorUser());
        document.setType(request.type());
        document.setTitle(trimToNull(request.title()));
        document.setDescription(trimToNull(request.description()));
        document.setFilePath(trimToNull(request.filePath()));
        document.setFileUrl(trimToNull(request.fileUrl()));
        document.setFileName(trimToNull(request.fileName()));
        document.setMimeType(trimToNull(request.mimeType()));
        document.setFileSize(request.fileSize());
        // Legacy upload endpoint represents an already issued attachment. The
        // new template editor uses DRAFT explicitly and never reaches this path.
        document.setStatus(com.qamqormed.entity.MedicalDocumentStatus.APPROVED);
        document.setFinalizedContent(trimToNull(request.description()));
        MedicalDocument saved = documents.save(document);
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.DOCUMENT_CREATED,
                "Добавлен новый документ", "Врач добавил новый медицинский документ.", appointment, "/patient/documents", "document-created-" + saved.getId());
        return MedicalDocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> nurseAppointments(Long userId) {
        return nurseAppointments(userId, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> nurseAppointments(Long userId, LocalDate date, AppointmentStatus status, String query) {
        DoctorNurseAssignment assignment = activeAssignmentForNurse(userId);
        return appointments.findAllByDoctorIdOrderByAppointmentAtAsc(assignment.getDoctor().getId()).stream()
                .filter(appointment -> matches(appointment, date, status, query))
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse nurseAppointment(Long userId, Long appointmentId) {
        return AppointmentResponse.from(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional(readOnly = true)
    public AppointmentDetailsResponse nurseAppointmentDetails(Long userId, Long appointmentId) {
        return nurseDetails(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> nurseSubjectHistory(Long userId, Long appointmentId) {
        return subjectHistory(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional
    public AppointmentDetailsResponse startNurseAssessment(Long userId, Long appointmentId) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.BOOKED || current == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.ARRIVED);
            appointment.setArrivedAt(java.time.Instant.now());
            current = AppointmentStatus.ARRIVED;
        }
        if (current != AppointmentStatus.ARRIVED && current != AppointmentStatus.PREPARING) {
            throw statusError("начать предварительный осмотр", appointment);
        }
        appointment.setStatus(AppointmentStatus.PREPARING);
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional
    public NurseIntakeResponse saveNurseIntake(Long userId, Long appointmentId, NurseIntakeRequest request) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.BOOKED || current == AppointmentStatus.ARRIVED) {
            appointment.setStatus(AppointmentStatus.PREPARING);
        } else if (current != AppointmentStatus.PREPARING) {
            throw statusError("изменить данные осмотра", appointment);
        }
        DoctorNurseAssignment assignment = activeAssignmentForNurse(userId);
        NurseIntake intake = intakes.findByAppointmentId(appointmentId).orElseGet(NurseIntake::new);
        intake.setAppointment(appointment);
        intake.setNurseUser(assignment.getNurse().getUser());
        intake.setTemperature(request.temperature());
        intake.setBloodPressure(trimToNull(request.bloodPressure()));
        intake.setBloodPressureSystolic(request.bloodPressureSystolic());
        intake.setBloodPressureDiastolic(request.bloodPressureDiastolic());
        intake.setPulse(request.pulse());
        intake.setOxygenSaturation(request.oxygenSaturation());
        intake.setWeight(request.weight());
        intake.setHeight(request.height());
        intake.setComplaintSummary(trimToNull(request.complaintSummary()));
        intake.setNurseNotes(trimToNull(firstNonBlank(request.nurseNotes(), request.notes())));
        intake.setNotes(trimToNull(request.notes()));
        appointments.save(appointment);
        return NurseIntakeResponse.from(intakes.save(intake));
    }

    @Transactional
    public AppointmentDetailsResponse readyForDoctor(Long userId, Long appointmentId) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.PREPARING);
        appointment.setStatus(AppointmentStatus.READY);
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional
    public AppointmentResponse checkInByNurse(Long userId, Long appointmentId) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.ARRIVED || current == AppointmentStatus.PREPARING || current == AppointmentStatus.READY) return AppointmentResponse.from(appointment);
        if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED) throw statusError("отметить прибытие", appointment);
        appointment.setStatus(AppointmentStatus.ARRIVED); appointment.setArrivedAt(java.time.Instant.now()); appointments.save(appointment);
        statusHistory.record(appointment, current, AppointmentStatus.ARRIVED, appointment.getBookedBy(), "nurse-check-in");
        notifications.medical(appointment.getBookedBy(), com.qamqormed.entity.NotificationType.PATIENT_ARRIVED,
                "Пациент прибыл", "Пациент отмечен как прибывший на приём.", appointment, "/patient/my-appointments", "patient-arrived-" + appointment.getId());
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse noShowByNurse(Long userId, Long appointmentId) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.NO_SHOW) return AppointmentResponse.from(appointment);
        if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED) throw statusError("отметить неявку", appointment);
        if (appointment.getAppointmentAt().isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Qyzylorda")))) throw new BadRequestException("Неявку можно отметить после времени записи");
        appointment.setStatus(AppointmentStatus.NO_SHOW); appointment.setNoShowAt(java.time.Instant.now()); appointments.save(appointment);
        statusHistory.record(appointment, current, AppointmentStatus.NO_SHOW, appointment.getBookedBy(), "nurse-no-show");
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<MedicalVisitResponse> patientHistory(Long userId, Long patientId) {
        patients.patient(userId);
        List<MedicalVisit> result;
        if (patientId == null || (!patients.isOwnedDependentId(userId, patientId)
                && (Objects.equals(userId, patientId) || patients.isOwnProfileId(userId, patientId)))) {
            result = visits.findAllByPatientUserIdAndDependentIsNullOrderByCompletedAtDesc(userId);
        } else {
            result = visits.findAllByDependentIdOrderByCompletedAtDesc(patients.dependent(userId, patientId).getId());
        }
        return result.stream().filter(visit -> visit.getCompletedAt() != null).map(item -> visitResponse(item, true)).toList();
    }

    @Transactional(readOnly = true)
    public MedicalVisitResponse patientResult(Long userId, Long appointmentId) {
        Appointment appointment = appointments.findByIdAndBookedById(appointmentId, userId)
                .orElseThrow(() -> new NotFoundException("Результат не найден"));
        if (appointment.getCanonicalStatus() != AppointmentStatus.COMPLETED) {
            throw new NotFoundException("Результат будет доступен после завершения приёма");
        }
        return visitResponse(visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Медицинский результат не найден")), true);
    }

    private AppointmentDetailsResponse details(Appointment appointment) {
        NurseIntakeResponse assessment = intakes.findByAppointmentId(appointment.getId()).map(NurseIntakeResponse::from).orElse(null);
        MedicalVisitResponse visit = visits.findByAppointmentId(appointment.getId()).map(this::visitResponse).orElse(null);
        return AppointmentDetailsResponse.from(appointment, assessment, visit);
    }

    /** Nurses receive the preparation context plus a deliberately reduced clinical view. */
    private AppointmentDetailsResponse nurseDetails(Appointment appointment) {
        NurseIntakeResponse assessment = intakes.findByAppointmentId(appointment.getId()).map(NurseIntakeResponse::from).orElse(null);
        MedicalVisitResponse full = visits.findByAppointmentId(appointment.getId()).map(this::visitResponse).orElse(null);
        MedicalVisitResponse limited = full == null ? null : new MedicalVisitResponse(
                full.id(), full.appointmentId(), full.subject(), full.doctor(), full.clinic(), full.appointmentAt(), full.appointmentStatus(),
                null, null, full.diagnosis(), full.diagnosisCode(), full.diagnosisName(), null, null,
                full.startedAt(), full.completedAt(), full.createdAt(), full.updatedAt(), List.of(), List.of(), List.of(),
                full.diagnoses(), List.of(), full.medications());
        return AppointmentDetailsResponse.from(appointment, assessment, limited);
    }

    /** Shared DTO mapping used by patient and doctor medical-card read models. */
    public MedicalVisitResponse visitResponse(MedicalVisit visit) {
        return visitResponse(visit, false);
    }

    public MedicalVisitResponse visitResponse(MedicalVisit visit, boolean approvedDocumentsOnly) {
        return MedicalVisitResponse.from(visit,
                treatments.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(TreatmentResponse::from).toList(),
                referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(ReferralResponse::from).toList(),
                documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                        .filter(item -> !approvedDocumentsOnly || item.getStatus() == null || item.getStatus() == MedicalDocumentStatus.APPROVED)
                        .map(item -> approvedDocumentsOnly ? MedicalDocumentResponse.fromPatient(item) : MedicalDocumentResponse.from(item)).toList(),
                visitDiagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream().map(com.qamqormed.dto.VisitDiagnosisResponse::from).toList(),
                structuredPrescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(com.qamqormed.dto.PrescriptionResponse::from).toList(),
                medicationPrescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(com.qamqormed.dto.MedicationPrescriptionResponse::from).toList());
    }

    private MedicalVisit visitFor(Appointment appointment, User doctorUser) {
        return visits.findByAppointmentId(appointment.getId()).orElseGet(() -> {
            MedicalVisit visit = new MedicalVisit();
            visit.setAppointment(appointment);
            visit.setPatientUser(documentOwner(appointment));
            visit.setDependent(appointment.getDependent());
            visit.setPatientProfile(appointment.getPatientProfile());
            visit.setDoctorProfile(appointment.getDoctor());
            visit.setClinic(appointment.getDoctor().getClinic());
            visit.setDoctorUser(doctorUser);
            return visit;
        });
    }

    private MedicalVisit requiredVisit(Long appointmentId) {
        return visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
    }

    private User documentOwner(MedicalVisit visit) {
        if (visit.getDependent() != null && visit.getDependent().getOwner() != null) return visit.getDependent().getOwner();
        if (visit.getPatientProfile() != null && visit.getPatientProfile().getUser() != null) return visit.getPatientProfile().getUser();
        return visit.getPatientUser();
    }

    private User documentOwner(Appointment appointment) {
        if (appointment.getDependent() != null && appointment.getDependent().getOwner() != null) return appointment.getDependent().getOwner();
        if (appointment.getPatientProfile() != null && appointment.getPatientProfile().getUser() != null) return appointment.getPatientProfile().getUser();
        return appointment.getBookedBy();
    }

    private void syncStructuredDiagnosis(MedicalVisit visit) {
        if (isBlank(visit.getDiagnosisCode())) return;
        boolean alreadySaved = visitDiagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream()
                .anyMatch(item -> item.getIcd10Code().getCode().equalsIgnoreCase(visit.getDiagnosisCode()));
        if (alreadySaved) return;
        icd10Codes.findByCodeIgnoreCase(visit.getDiagnosisCode()).ifPresent(code -> {
            com.qamqormed.entity.VisitDiagnosis diagnosis = new com.qamqormed.entity.VisitDiagnosis();
            diagnosis.setMedicalVisit(visit);
            diagnosis.setIcd10Code(code);
            diagnosis.setNotes(visit.getDiagnosis());
            diagnosis.setPrimaryDiagnosis(true);
            visitDiagnoses.save(diagnosis);
        });
    }

    private void applyVisit(MedicalVisit visit, MedicalVisitUpsertRequest request) {
        visit.setChiefComplaint(trimToNull(request.chiefComplaint()));
        visit.setAnamnesis(trimToNull(request.anamnesis()));
        visit.setDiagnosis(trimToNull(request.diagnosis()));
        visit.setDiagnosisCode(trimToNull(request.diagnosisCode()));
        visit.setDiagnosisName(trimToNull(request.diagnosisName()));
        visit.setDoctorNotes(trimToNull(request.doctorNotes()));
        visit.setRecommendations(trimToNull(request.recommendations()));
    }

    private void applyOwnership(Treatment treatment, MedicalVisit visit) {
        treatment.setMedicalVisit(visit);
        treatment.setPatientUser(visit.getPatientUser());
        treatment.setDependent(visit.getDependent());
        treatment.setDoctorUser(visit.getDoctorUser());
    }

    private void applyOwnership(Referral referral, MedicalVisit visit) {
        referral.setMedicalVisit(visit);
        referral.setPatientUser(visit.getPatientUser());
        referral.setDependent(visit.getDependent());
        referral.setDoctorUser(visit.getDoctorUser());
    }

    private MedicalRecord syncLegacyRecord(MedicalVisit visit) {
        MedicalRecord record = records.findByAppointmentId(visit.getAppointment().getId()).orElseGet(MedicalRecord::new);
        record.setAppointment(visit.getAppointment());
        record.setPatientUser(visit.getPatientUser());
        record.setDependent(visit.getDependent());
        record.setDoctorUser(visit.getDoctorUser());
        record.setDiagnosis(visit.getDiagnosis());
        record.setRecommendations(visit.getRecommendations());
        record.setPrescriptions(treatments.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .map(item -> item.getName() + (isBlank(item.getDosage()) ? "" : " — " + item.getDosage()))
                .reduce((left, right) -> left + "; " + right).orElse(null));
        record.setReferrals(referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .map(item -> item.getReason() == null ? item.getReferralType().name() : item.getReason())
                .reduce((left, right) -> left + "; " + right).orElse(null));
        return records.save(record);
    }

    private List<MedicalRecordResponse> subjectHistory(Appointment appointment) {
        List<MedicalRecord> history = appointment.getDependent() == null
                ? records.findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(appointment.getBookedBy().getId())
                : records.findAllByDependentIdOrderByCreatedAtDesc(appointment.getDependent().getId());
        return history.stream().map(MedicalRecordResponse::from).toList();
    }

    private DoctorPatientSummaryResponse patientSummary(List<Appointment> appointmentsForSubject) {
        Appointment first = appointmentsForSubject.getFirst();
        AppointmentResponse summary = AppointmentResponse.from(first);
        LocalDateTime now = LocalDateTime.now();
        LocalDate last = appointmentsForSubject.stream()
                .map(Appointment::getAppointmentAt)
                .filter(value -> value.isBefore(now))
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
        LocalDate next = appointmentsForSubject.stream()
                .map(Appointment::getAppointmentAt)
                .filter(value -> !value.isBefore(now))
                .min(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
        String lastDiagnosis = appointmentsForSubject.stream()
                .sorted(Comparator.comparing(Appointment::getAppointmentAt).reversed())
                .map(item -> visits.findByAppointmentId(item.getId()).map(MedicalVisit::getDiagnosis).orElse(null))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        return new DoctorPatientSummaryResponse(summary.subject(), subjectPhone(first), subjectEmail(first),
                ownerFullName(first), appointmentsForSubject.size(), last, next, lastDiagnosis);
    }

    private boolean matchesPatientFilter(Appointment appointment, String query, AppointmentStatus status,
                                         LocalDate date, String period, String type, Long clinicId) {
        if (status != null && canonical(status) != canonical(appointment.getStatus())) return false;
        if (date != null && !date.equals(appointment.getAppointmentAt().toLocalDate())) return false;
        if (period != null && !period.isBlank() && !matchesPeriod(appointment.getAppointmentAt().toLocalDate(), period)) return false;
        if (clinicId != null && (appointment.getDoctor().getClinic() == null
                || !clinicId.equals(appointment.getDoctor().getClinic().getId()))) return false;
        if (type != null && !type.isBlank()
                && !type.equalsIgnoreCase(appointment.getDependent() == null ? "SELF" : "DEPENDENT")) return false;
        if (query == null || query.isBlank()) return true;
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return subjectName(appointment).toLowerCase(Locale.ROOT).contains(normalized)
                || valueOf(subjectIin(appointment)).contains(normalized);
    }

    private boolean matchesPeriod(LocalDate appointmentDate, String period) {
        LocalDate today = LocalDate.now();
        return switch (period.toUpperCase(Locale.ROOT)) {
            case "TODAY" -> appointmentDate.equals(today);
            case "WEEK" -> !appointmentDate.isBefore(today) && appointmentDate.isBefore(today.plusDays(7));
            case "MONTH" -> !appointmentDate.isBefore(today) && appointmentDate.isBefore(today.plusDays(30));
            default -> true;
        };
    }

    private boolean subjectMatches(Appointment appointment, Long patientId, String type) {
        boolean dependent = appointment.getDependent() != null;
        if (type != null && !type.isBlank()
                && !type.equalsIgnoreCase(dependent ? "DEPENDENT" : "SELF")) return false;
        if (dependent) return patientId.equals(appointment.getDependent().getId());
        Long selfId = appointment.getPatientProfile() == null
                ? appointment.getBookedBy().getId()
                : appointment.getPatientProfile().getUser().getId();
        return patientId.equals(selfId);
    }

    private String subjectKey(Appointment appointment) {
        return appointment.getDependent() == null
                ? "SELF:" + (appointment.getPatientProfile() == null
                ? appointment.getBookedBy().getId() : appointment.getPatientProfile().getUser().getId())
                : "DEPENDENT:" + appointment.getDependent().getId();
    }

    private String subjectIin(Appointment appointment) {
        if (appointment.getDependent() != null) return appointment.getDependent().getIin();
        if (appointment.getPatientProfile() != null && appointment.getPatientProfile().getIin() != null) {
            return appointment.getPatientProfile().getIin();
        }
        return appointment.getBookedBy().getIin();
    }

    private String subjectPhone(Appointment appointment) {
        if (appointment.getDependent() != null && appointment.getDependent().getPhone() != null) {
            return appointment.getDependent().getPhone();
        }
        if (appointment.getPatientProfile() != null && appointment.getPatientProfile().getPhone() != null) {
            return appointment.getPatientProfile().getPhone();
        }
        return appointment.getBookedBy().getPhone();
    }

    private String subjectEmail(Appointment appointment) { return appointment.getBookedBy().getEmail(); }

    private String ownerFullName(Appointment appointment) {
        return appointment.getDependent() == null ? null : fullName(appointment.getBookedBy());
    }

    private AppointmentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try { return AppointmentStatus.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private String valueOf(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }

    private boolean dateInRange(java.time.Instant value, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        LocalDate date = (value == null ? java.time.Instant.now() : value)
                .atZone(java.time.ZoneId.of("Asia/Qyzylorda")).toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private String fullName(User user) {
        return String.join(" ", user.getLastName(), user.getFirstName(), user.getMiddleName() == null ? "" : user.getMiddleName())
                .trim().replaceAll(" +", " ");
    }

    private boolean matches(Appointment appointment, LocalDate date, AppointmentStatus status, String query) {
        if (date != null && !date.equals(appointment.getAppointmentAt().toLocalDate())) return false;
        if (status != null && canonical(status) != canonical(appointment.getStatus())) return false;
        if (query == null || query.isBlank()) return true;
        return subjectName(appointment).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private String subjectName(Appointment appointment) {
        if (appointment.getDependent() != null) {
            return String.join(" ", appointment.getDependent().getLastName(), appointment.getDependent().getFirstName(),
                    appointment.getDependent().getMiddleName() == null ? "" : appointment.getDependent().getMiddleName());
        }
        return String.join(" ", appointment.getBookedBy().getLastName(), appointment.getBookedBy().getFirstName(),
                appointment.getBookedBy().getMiddleName() == null ? "" : appointment.getBookedBy().getMiddleName());
    }

    private void transition(Appointment appointment, AppointmentStatus target) {
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == target) return;
        boolean allowed = switch (current) {
            case BOOKED -> target == AppointmentStatus.CONFIRMED || target == AppointmentStatus.ARRIVED || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case CONFIRMED -> target == AppointmentStatus.ARRIVED || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case ARRIVED -> target == AppointmentStatus.PREPARING || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case PREPARING -> target == AppointmentStatus.READY || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case READY -> target == AppointmentStatus.IN_APPOINTMENT || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case IN_APPOINTMENT -> target == AppointmentStatus.COMPLETED || target == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
            default -> false;
        };
        if (!allowed) throw statusError("изменить статус", appointment);
        appointment.setStatus(target);
        applyStatusTimestamp(appointment, target);
        statusHistory.record(appointment, current, target, null, null);
    }

    private void applyStatusTimestamp(Appointment appointment, AppointmentStatus target) {
        java.time.Instant now = java.time.Instant.now();
        switch (target) {
            case CONFIRMED -> appointment.setConfirmedAt(now);
            case ARRIVED -> appointment.setArrivedAt(now);
            case IN_APPOINTMENT -> appointment.setStartedAt(now);
            case COMPLETED -> appointment.setCompletedAt(now);
            case NO_SHOW -> appointment.setNoShowAt(now);
            default -> { }
        }
    }

    private void requireStatus(Appointment appointment, AppointmentStatus expected) {
        if (canonical(appointment.getStatus()) != expected) {
            String action = expected == AppointmentStatus.IN_APPOINTMENT ? "начать медицинский приём" : "выполнить операцию";
            throw statusError(action, appointment);
        }
    }

    private BadRequestException statusError(String action, Appointment appointment) {
        return new BadRequestException("Нельзя " + action + ": текущий статус записи — " + canonical(appointment.getStatus()).name());
    }

    private AppointmentStatus canonical(AppointmentStatus status) { return status == null ? AppointmentStatus.BOOKED : status.canonical(); }

    private DoctorProfile doctorForUser(Long userId) {
        return doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
    }

    private DoctorNurseAssignment activeAssignmentForNurse(Long userId) {
        NurseProfile nurse = nurses.findByUserId(userId).filter(NurseProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль медсестры не найден"));
        return assignments.findActiveByNurseId(nurse.getId())
                .orElseThrow(() -> new NotFoundException("За медсестрой не закреплён врач"));
    }

    private Appointment doctorAppointmentEntity(Long userId, Long appointmentId) {
        DoctorProfile doctor = doctorForUser(userId);
        return appointments.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
    }

    private Appointment nurseAppointmentEntity(Long userId, Long appointmentId) {
        DoctorNurseAssignment assignment = activeAssignmentForNurse(userId);
        return appointments.findByIdAndDoctorId(appointmentId, assignment.getDoctor().getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
    }

    private String firstNonBlank(String first, String second) { return isBlank(first) ? second : first; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String trimToNull(String value) { return isBlank(value) ? null : value.trim(); }
}
