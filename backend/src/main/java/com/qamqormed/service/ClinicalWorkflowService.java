package com.qamqormed.service;

import com.qamqormed.dto.AppointmentDetailsResponse;
import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.MedicalDocumentRequest;
import com.qamqormed.dto.MedicalDocumentResponse;
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
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.MedicalVisitRepository;
import com.qamqormed.repository.NurseIntakeRepository;
import com.qamqormed.repository.NurseProfileRepository;
import com.qamqormed.repository.ReferralRepository;
import com.qamqormed.repository.SpecialtyRepository;
import com.qamqormed.repository.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ClinicalWorkflowService {
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

    public ClinicalWorkflowService(DoctorProfileRepository doctors, NurseProfileRepository nurses,
                                   DoctorNurseAssignmentRepository assignments, AppointmentRepository appointments,
                                   MedicalRecordRepository records, NurseIntakeRepository intakes,
                                   MedicalVisitRepository visits, TreatmentRepository treatments,
                                   ReferralRepository referrals, MedicalDocumentRepository documents,
                                   SpecialtyRepository specialties, ClinicRepository clinics, PatientService patients) {
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

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> doctorSubjectHistory(Long userId, Long appointmentId) {
        return subjectHistory(doctorAppointmentEntity(userId, appointmentId));
    }

    @Transactional
    public AppointmentDetailsResponse startDoctorVisit(Long userId, Long appointmentId) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.READY_FOR_DOCTOR);
        MedicalVisit visit = visitFor(appointment, appointment.getDoctor().getUser());
        if (visit.getStartedAt() == null) visit.setStartedAt(LocalDateTime.now());
        visits.save(visit);
        appointment.setStatus(AppointmentStatus.DOCTOR_IN_PROGRESS);
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional
    public MedicalVisitResponse saveMedicalVisit(Long userId, Long appointmentId, MedicalVisitUpsertRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.DOCTOR_IN_PROGRESS);
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
        return MedicalRecordResponse.from(syncLegacyRecord(visit));
    }

    @Transactional
    public AppointmentResponse updateDoctorStatus(Long userId, Long appointmentId, AppointmentStatusRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        AppointmentStatus target = canonical(request.status());
        if (target == AppointmentStatus.DOCTOR_IN_PROGRESS) {
            startDoctorVisit(userId, appointmentId);
        } else {
            transition(appointment, target);
            appointments.save(appointment);
        }
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentDetailsResponse completeDoctorVisit(Long userId, Long appointmentId) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.DOCTOR_IN_PROGRESS);
        MedicalVisit visit = visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
        if (isBlank(visit.getDiagnosis())) throw new BadRequestException("Перед завершением укажите диагноз");
        visit.setCompletedAt(LocalDateTime.now());
        visits.save(visit);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointments.save(appointment);
        syncLegacyRecord(visit);
        return details(appointment);
    }

    @Transactional
    public TreatmentResponse addTreatment(Long userId, Long appointmentId, TreatmentRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.DOCTOR_IN_PROGRESS);
        if (isBlank(request.name())) throw new BadRequestException("Укажите название препарата или назначения");
        MedicalVisit visit = requiredVisit(appointmentId);
        Treatment treatment = new Treatment();
        applyOwnership(treatment, visit);
        treatment.setName(trimToNull(request.name()));
        treatment.setDosage(trimToNull(request.dosage()));
        treatment.setFrequency(trimToNull(request.frequency()));
        treatment.setDuration(trimToNull(request.duration()));
        treatment.setInstructions(trimToNull(request.instructions()));
        return TreatmentResponse.from(treatments.save(treatment));
    }

    @Transactional
    public ReferralResponse addReferral(Long userId, Long appointmentId, ReferralRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.DOCTOR_IN_PROGRESS);
        if (request.referralType() == null) throw new BadRequestException("Укажите тип направления");
        MedicalVisit visit = requiredVisit(appointmentId);
        Referral referral = new Referral();
        applyOwnership(referral, visit);
        referral.setReferralType(request.referralType());
        referral.setTargetSpecialty(request.targetSpecialtyId() == null ? null : specialties.findById(request.targetSpecialtyId())
                .orElseThrow(() -> new NotFoundException("Специальность направления не найдена")));
        referral.setTargetClinic(request.targetClinicId() == null ? null : clinics.findById(request.targetClinicId())
                .orElseThrow(() -> new NotFoundException("Клиника направления не найдена")));
        referral.setReason(trimToNull(request.reason()));
        referral.setNotes(trimToNull(request.notes()));
        referral.setStatus(ReferralStatus.CREATED);
        return ReferralResponse.from(referrals.save(referral));
    }

    @Transactional
    public MedicalDocumentResponse addDocument(Long userId, Long appointmentId, MedicalDocumentRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.DOCTOR_IN_PROGRESS);
        if (request.type() == null || isBlank(request.title())) throw new BadRequestException("Укажите тип и название документа");
        MedicalVisit visit = requiredVisit(appointmentId);
        MedicalDocument document = new MedicalDocument();
        document.setMedicalVisit(visit);
        document.setPatientUser(visit.getPatientUser());
        document.setDependent(visit.getDependent());
        document.setDoctorUser(visit.getDoctorUser());
        document.setType(request.type());
        document.setTitle(trimToNull(request.title()));
        document.setDescription(trimToNull(request.description()));
        document.setFilePath(trimToNull(request.filePath()));
        document.setFileUrl(trimToNull(request.fileUrl()));
        return MedicalDocumentResponse.from(documents.save(document));
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
        return details(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> nurseSubjectHistory(Long userId, Long appointmentId) {
        return subjectHistory(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional
    public AppointmentDetailsResponse startNurseAssessment(Long userId, Long appointmentId) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.BOOKED) {
            appointment.setStatus(AppointmentStatus.CHECKED_IN);
            current = AppointmentStatus.CHECKED_IN;
        }
        if (current != AppointmentStatus.CHECKED_IN && current != AppointmentStatus.NURSE_IN_PROGRESS) {
            throw statusError("начать предварительный осмотр", appointment);
        }
        appointment.setStatus(AppointmentStatus.NURSE_IN_PROGRESS);
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional
    public NurseIntakeResponse saveNurseIntake(Long userId, Long appointmentId, NurseIntakeRequest request) {
        Appointment appointment = nurseAppointmentEntity(userId, appointmentId);
        AppointmentStatus current = canonical(appointment.getStatus());
        if (current == AppointmentStatus.BOOKED || current == AppointmentStatus.CHECKED_IN) {
            appointment.setStatus(AppointmentStatus.NURSE_IN_PROGRESS);
        } else if (current != AppointmentStatus.NURSE_IN_PROGRESS) {
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
        requireStatus(appointment, AppointmentStatus.NURSE_IN_PROGRESS);
        appointment.setStatus(AppointmentStatus.READY_FOR_DOCTOR);
        appointments.save(appointment);
        return details(appointment);
    }

    @Transactional(readOnly = true)
    public List<MedicalVisitResponse> patientHistory(Long userId, Long patientId) {
        patients.patient(userId);
        List<MedicalVisit> result;
        if (Objects.equals(userId, patientId)) {
            result = visits.findAllByPatientUserIdAndDependentIsNullOrderByCompletedAtDesc(userId);
        } else {
            result = visits.findAllByDependentIdOrderByCompletedAtDesc(patients.dependent(userId, patientId).getId());
        }
        return result.stream().filter(visit -> visit.getCompletedAt() != null).map(this::visitResponse).toList();
    }

    @Transactional(readOnly = true)
    public MedicalVisitResponse patientResult(Long userId, Long appointmentId) {
        Appointment appointment = appointments.findByIdAndBookedById(appointmentId, userId)
                .orElseThrow(() -> new NotFoundException("Результат не найден"));
        if (appointment.getCanonicalStatus() != AppointmentStatus.COMPLETED) {
            throw new NotFoundException("Результат будет доступен после завершения приёма");
        }
        return visitResponse(visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Медицинский результат не найден")));
    }

    private AppointmentDetailsResponse details(Appointment appointment) {
        NurseIntakeResponse assessment = intakes.findByAppointmentId(appointment.getId()).map(NurseIntakeResponse::from).orElse(null);
        MedicalVisitResponse visit = visits.findByAppointmentId(appointment.getId()).map(this::visitResponse).orElse(null);
        return AppointmentDetailsResponse.from(appointment, assessment, visit);
    }

    private MedicalVisitResponse visitResponse(MedicalVisit visit) {
        return MedicalVisitResponse.from(visit,
                treatments.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(TreatmentResponse::from).toList(),
                referrals.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(ReferralResponse::from).toList(),
                documents.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream().map(MedicalDocumentResponse::from).toList());
    }

    private MedicalVisit visitFor(Appointment appointment, User doctorUser) {
        return visits.findByAppointmentId(appointment.getId()).orElseGet(() -> {
            MedicalVisit visit = new MedicalVisit();
            visit.setAppointment(appointment);
            visit.setPatientUser(appointment.getBookedBy());
            visit.setDependent(appointment.getDependent());
            visit.setDoctorUser(doctorUser);
            return visit;
        });
    }

    private MedicalVisit requiredVisit(Long appointmentId) {
        return visits.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
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
            case BOOKED -> target == AppointmentStatus.CHECKED_IN || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case CHECKED_IN -> target == AppointmentStatus.NURSE_IN_PROGRESS || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case NURSE_IN_PROGRESS -> target == AppointmentStatus.READY_FOR_DOCTOR || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case READY_FOR_DOCTOR -> target == AppointmentStatus.DOCTOR_IN_PROGRESS || target == AppointmentStatus.CANCELLED || target == AppointmentStatus.NO_SHOW;
            case DOCTOR_IN_PROGRESS -> target == AppointmentStatus.COMPLETED || target == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
            default -> false;
        };
        if (!allowed) throw statusError("изменить статус", appointment);
        appointment.setStatus(target);
    }

    private void requireStatus(Appointment appointment, AppointmentStatus expected) {
        if (canonical(appointment.getStatus()) != expected) {
            String action = expected == AppointmentStatus.DOCTOR_IN_PROGRESS ? "начать медицинский приём" : "выполнить операцию";
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
