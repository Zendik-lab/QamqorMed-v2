package com.qamqormed.service;

import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.dto.AppointmentStatusRequest;
import com.qamqormed.dto.MedicalRecordCreateRequest;
import com.qamqormed.dto.MedicalRecordResponse;
import com.qamqormed.dto.NurseIntakeRequest;
import com.qamqormed.dto.NurseIntakeResponse;
import com.qamqormed.entity.Appointment;
import com.qamqormed.entity.AppointmentStatus;
import com.qamqormed.entity.DoctorNurseAssignment;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.MedicalRecord;
import com.qamqormed.entity.NurseIntake;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.entity.User;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DoctorNurseAssignmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.MedicalRecordRepository;
import com.qamqormed.repository.NurseIntakeRepository;
import com.qamqormed.repository.NurseProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClinicalWorkflowService {
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final AppointmentRepository appointments;
    private final MedicalRecordRepository records;
    private final NurseIntakeRepository intakes;

    public ClinicalWorkflowService(DoctorProfileRepository doctors, NurseProfileRepository nurses,
                                   DoctorNurseAssignmentRepository assignments, AppointmentRepository appointments,
                                   MedicalRecordRepository records, NurseIntakeRepository intakes) {
        this.doctors = doctors;
        this.nurses = nurses;
        this.assignments = assignments;
        this.appointments = appointments;
        this.records = records;
        this.intakes = intakes;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> doctorAppointments(Long userId) {
        DoctorProfile doctor = doctorForUser(userId);
        return appointments.findAllByDoctorIdOrderByAppointmentAtAsc(doctor.getId()).stream().map(AppointmentResponse::from).toList();
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
    public MedicalRecordResponse saveMedicalRecord(Long userId, Long appointmentId, MedicalRecordCreateRequest request) {
        DoctorProfile doctor = doctorForUser(userId);
        Appointment appointment = appointments.findByIdAndDoctorId(appointmentId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) throw new BadRequestException("Отменённая запись недоступна");
        MedicalRecord record = records.findByAppointmentId(appointmentId).orElseGet(MedicalRecord::new);
        record.setAppointment(appointment);
        record.setPatientUser(appointment.getBookedBy());
        record.setDependent(appointment.getDependent());
        record.setDoctorUser(doctor.getUser());
        record.setDiagnosis(trimToNull(request.diagnosis()));
        record.setRecommendations(trimToNull(request.recommendations()));
        record.setPrescriptions(trimToNull(request.prescriptions()));
        record.setReferrals(trimToNull(request.referrals()));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointments.save(appointment);
        return MedicalRecordResponse.from(records.save(record));
    }

    @Transactional
    public AppointmentResponse updateDoctorStatus(Long userId, Long appointmentId, AppointmentStatusRequest request) {
        Appointment appointment = doctorAppointmentEntity(userId, appointmentId);
        appointment.setStatus(request.status());
        return AppointmentResponse.from(appointments.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> nurseAppointments(Long userId) {
        DoctorNurseAssignment assignment = activeAssignmentForNurse(userId);
        return appointments.findAllByDoctorIdOrderByAppointmentAtAsc(assignment.getDoctor().getId()).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse nurseAppointment(Long userId, Long appointmentId) {
        return AppointmentResponse.from(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> nurseSubjectHistory(Long userId, Long appointmentId) {
        return subjectHistory(nurseAppointmentEntity(userId, appointmentId));
    }

    @Transactional
    public NurseIntakeResponse saveNurseIntake(Long userId, Long appointmentId, NurseIntakeRequest request) {
        DoctorNurseAssignment assignment = activeAssignmentForNurse(userId);
        Appointment appointment = appointments.findByIdAndDoctorId(appointmentId, assignment.getDoctor().getId())
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) throw new BadRequestException("Отменённая запись недоступна");
        NurseIntake intake = intakes.findByAppointmentId(appointmentId).orElseGet(NurseIntake::new);
        intake.setAppointment(appointment);
        intake.setNurseUser(assignment.getNurse().getUser());
        intake.setTemperature(request.temperature());
        intake.setBloodPressure(trimToNull(request.bloodPressure()));
        intake.setPulse(request.pulse());
        intake.setWeight(request.weight());
        intake.setHeight(request.height());
        intake.setNotes(trimToNull(request.notes()));
        if (appointment.getStatus() == AppointmentStatus.SCHEDULED) appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointments.save(appointment);
        return NurseIntakeResponse.from(intakes.save(intake));
    }

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

    private List<MedicalRecordResponse> subjectHistory(Appointment appointment) {
        List<MedicalRecord> history = appointment.getDependent() == null
                ? records.findAllByPatientUserIdAndDependentIsNullOrderByCreatedAtDesc(appointment.getBookedBy().getId())
                : records.findAllByDependentIdOrderByCreatedAtDesc(appointment.getDependent().getId());
        return history.stream().map(MedicalRecordResponse::from).toList();
    }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
