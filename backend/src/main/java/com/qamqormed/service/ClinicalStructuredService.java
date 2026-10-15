package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ForbiddenException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/** Structured clinical data operations kept separate from appointment state transitions. */
@Service
public class ClinicalStructuredService {
    private final ICD10CodeRepository codes;
    private final VisitDiagnosisRepository diagnoses;
    private final PrescriptionRepository prescriptions;
    private final MedicationPrescriptionRepository medications;
    private final MedicalVisitRepository visits;
    private final AppointmentRepository appointments;
    private final DoctorProfileRepository doctors;
    private final PatientService patients;
    private final LabResultRepository labResults;
    private final MedicalCertificateRepository certificates;

    public ClinicalStructuredService(ICD10CodeRepository codes, VisitDiagnosisRepository diagnoses,
                                     PrescriptionRepository prescriptions, MedicationPrescriptionRepository medications,
                                     MedicalVisitRepository visits, AppointmentRepository appointments,
                                     DoctorProfileRepository doctors, PatientService patients,
                                     LabResultRepository labResults, MedicalCertificateRepository certificates) {
        this.codes = codes; this.diagnoses = diagnoses; this.prescriptions = prescriptions;
        this.medications = medications; this.visits = visits; this.appointments = appointments;
        this.doctors = doctors; this.patients = patients; this.labResults = labResults; this.certificates = certificates;
    }

    @Transactional(readOnly = true)
    public List<ICD10CodeResponse> searchCodes(String query) {
        String normalized = query == null ? "" : query.trim();
        return (normalized.isBlank()
                ? codes.findAll(PageRequest.of(0, 50)).stream().filter(item -> item.isActive() && item.isSelectable()).toList()
                : codes.search(normalized, PageRequest.of(0, 50)).getContent())
                .stream().map(ICD10CodeResponse::from).toList();
    }

    @Transactional
    public VisitDiagnosisResponse addDiagnosis(Long userId, Long appointmentId, VisitDiagnosisRequest request) {
        MedicalVisit visit = doctorVisit(userId, appointmentId);
        ICD10Code code = codes.findById(request.icd10CodeId()).filter(ICD10Code::isActive)
                .orElseThrow(() -> new NotFoundException("Код МКБ-10 не найден"));
        if (request.primary()) {
            diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).forEach(item -> item.setPrimaryDiagnosis(false));
        }
        VisitDiagnosis diagnosis = new VisitDiagnosis();
        diagnosis.setMedicalVisit(visit); diagnosis.setIcd10Code(code);
        diagnosis.setIcd10CodeSnapshot(code.getCode());
        diagnosis.setIcd10NameSnapshot(code.getName());
        diagnosis.setNotes(trim(request.notes())); diagnosis.setPrimaryDiagnosis(request.primary());
        diagnosis.setDiagnosisType(request.diagnosisType() == null
                ? (request.primary() ? DiagnosisType.PRIMARY : DiagnosisType.SECONDARY)
                : request.diagnosisType());
        // Keep the legacy human-readable field synchronized so the existing
        // completion guard also supports the structured ICD-10 workflow.
        if (blank(visit.getDiagnosis())) visit.setDiagnosis(code.getCode() + " — " + code.getName());
        return VisitDiagnosisResponse.from(diagnoses.save(diagnosis));
    }

    @Transactional(readOnly = true)
    public List<VisitDiagnosisResponse> diagnosesForDoctor(Long userId, Long appointmentId) {
        return diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(doctorVisit(userId, appointmentId).getId())
                .stream().map(VisitDiagnosisResponse::from).toList();
    }

    @Transactional
    public PrescriptionResponse addPrescription(Long userId, Long appointmentId, PrescriptionCreateRequest request) {
        MedicalVisit visit = doctorVisit(userId, appointmentId);
        if (blank(request.title())) throw new BadRequestException("Укажите название назначения");
        Prescription value = new Prescription(); value.setMedicalVisit(visit); value.setTitle(trim(request.title()));
        value.setDescription(trim(request.description())); value.setInstructions(trim(request.instructions()));
        value.setStartDate(request.startDate()); value.setEndDate(request.endDate());
        return PrescriptionResponse.from(prescriptions.save(value));
    }

    @Transactional
    public MedicationPrescriptionResponse addMedication(Long userId, Long appointmentId, MedicationPrescriptionRequest request) {
        MedicalVisit visit = doctorVisit(userId, appointmentId);
        if (blank(request.medicationName())) throw new BadRequestException("Укажите название лекарства");
        MedicationPrescription value = new MedicationPrescription(); value.setMedicalVisit(visit);
        value.setMedicationName(trim(request.medicationName())); value.setDosage(trim(request.dosage()));
        value.setDosageUnit(trim(request.dosageUnit())); value.setFrequency(trim(request.frequency())); value.setRoute(trim(request.route()));
        value.setDuration(trim(request.duration())); value.setDurationDays(request.durationDays());
        value.setInstructions(trim(request.instructions())); value.setStartDate(request.startDate()); value.setEndDate(request.endDate());
        return MedicationPrescriptionResponse.from(medications.save(value));
    }

    @Transactional(readOnly = true)
    public List<VisitDiagnosisResponse> patientDiagnoses(Long userId, Long subjectId) {
        return subjectVisits(userId, subjectId).stream().flatMap(visit -> diagnoses.findAllByMedicalVisitIdOrderByPrimaryDiagnosisDescCreatedAtAsc(visit.getId()).stream())
                .map(VisitDiagnosisResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> patientPrescriptions(Long userId, Long subjectId) {
        return subjectVisits(userId, subjectId).stream().flatMap(visit -> prescriptions.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream())
                .map(PrescriptionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationPrescriptionResponse> patientMedications(Long userId, Long subjectId) {
        return subjectVisits(userId, subjectId).stream().flatMap(visit -> medications.findAllByMedicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream())
                .map(MedicationPrescriptionResponse::from).toList();
    }

    @Transactional
    public LabResultResponse addLabResult(Long userId, Long appointmentId, LabResultCreateRequest request) {
        MedicalVisit visit = doctorVisit(userId, appointmentId);
        LabResult result = new LabResult();
        result.setMedicalVisit(visit);
        result.setDoctor(visit.getDoctorProfile());
        result.setPatientProfile(visit.getPatientProfile());
        result.setDependent(visit.getDependent());
        result.setTestName(request.testName().trim());
        result.setTestCode(trim(request.testCode()));
        result.setTestDate(request.testDate() == null ? java.time.LocalDate.now() : request.testDate());
        result.setStatus(request.status().trim().toUpperCase(java.util.Locale.ROOT));
        result.setResult(trim(request.result()));
        result.setReferenceRange(trim(request.referenceRange()));
        result.setComment(trim(request.comment()));
        if ("COMPLETED".equals(result.getStatus())) result.setCompletedAt(java.time.Instant.now());
        return LabResultResponse.from(labResults.save(result));
    }

    @Transactional
    public MedicalCertificateResponse addCertificate(Long userId, Long appointmentId, MedicalCertificateCreateRequest request) {
        MedicalVisit visit = doctorVisit(userId, appointmentId);
        MedicalCertificate certificate = new MedicalCertificate();
        certificate.setMedicalVisit(visit);
        certificate.setDoctor(visit.getDoctorProfile());
        certificate.setPatientProfile(visit.getPatientProfile());
        certificate.setDependent(visit.getDependent());
        certificate.setType(request.type().trim());
        certificate.setTitle(trim(request.title()));
        certificate.setValidFrom(request.validFrom());
        certificate.setValidUntil(request.validUntil());
        certificate.setDocumentNumber(trim(request.documentNumber()));
        if (request.status() != null && !request.status().isBlank()) certificate.setStatus(request.status().trim().toUpperCase(java.util.Locale.ROOT));
        certificate.setDescription(trim(request.description()));
        return MedicalCertificateResponse.from(certificates.save(certificate));
    }

    @Transactional(readOnly = true)
    public List<LabResultResponse> patientLabResults(Long userId, Long subjectId) {
        if (selfSubject(userId, subjectId)) {
            return labResults.findAllByPatientProfileIdOrderByTestDateDesc(patients.profileEntity(userId).getId())
                    .stream().map(LabResultResponse::from).toList();
        }
        ownedDependent(userId, subjectId);
        return labResults.findAllByDependentIdOrderByTestDateDesc(subjectId).stream().map(LabResultResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalCertificateResponse> patientCertificates(Long userId, Long subjectId) {
        if (selfSubject(userId, subjectId)) {
            return certificates.findAllByPatientProfileIdOrderByIssuedAtDesc(patients.profileEntity(userId).getId())
                    .stream().map(MedicalCertificateResponse::from).toList();
        }
        ownedDependent(userId, subjectId);
        return certificates.findAllByDependentIdOrderByIssuedAtDesc(subjectId).stream().map(MedicalCertificateResponse::from).toList();
    }

    private MedicalVisit doctorVisit(Long userId, Long appointmentId) {
        DoctorProfile doctor = doctors.findByUserId(userId).filter(DoctorProfile::isActive)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
        Appointment appointment = appointments.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        if (appointment.getCanonicalStatus() != AppointmentStatus.IN_APPOINTMENT) {
            throw new BadRequestException("Сначала начните медицинский приём");
        }
        return visits.findByAppointmentId(appointmentId).orElseThrow(() -> new BadRequestException("Сначала начните медицинский приём"));
    }

    private List<MedicalVisit> subjectVisits(Long userId, Long subjectId) {
        patients.patient(userId);
        if (selfSubject(userId, subjectId)) return visits.findAllByPatientUserIdAndDependentIsNullOrderByCompletedAtDesc(userId);
        ownedDependent(userId, subjectId);
        return visits.findAllByDependentIdOrderByCompletedAtDesc(subjectId);
    }

    private boolean selfSubject(Long userId, Long subjectId) {
        if (subjectId == null) return true;
        if (patients.isOwnedDependentId(userId, subjectId)) return false;
        if (subjectId.equals(userId)) return true;
        return patients.isOwnProfileId(userId, subjectId);
    }

    private void ownedDependent(Long userId, Long subjectId) {
        try {
            patients.dependent(userId, subjectId);
        } catch (com.qamqormed.exception.NotFoundException exception) {
            throw new ForbiddenException("Нет доступа к медицинской карте этого пациента");
        }
    }

    private String trim(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
