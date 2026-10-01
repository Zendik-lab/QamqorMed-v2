package com.qamqormed.service;

import com.qamqormed.dto.AppointmentCreateRequest;
import com.qamqormed.dto.AppointmentResponse;
import com.qamqormed.entity.Appointment;
import com.qamqormed.entity.Dependent;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.User;
import com.qamqormed.entity.AppointmentStatus;
import com.qamqormed.entity.TimeSlot;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AppointmentService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");
    private final AppointmentRepository appointments;
    private final DoctorProfileRepository doctors;
    private final UserRepository users;
    private final PatientService patients;
    private final TimeSlotService timeSlots;
    private final ApplicationEventPublisher events;

    public AppointmentService(AppointmentRepository appointments, DoctorProfileRepository doctors, UserRepository users,
                               PatientService patients, TimeSlotService timeSlots, ApplicationEventPublisher events) {
        this.appointments = appointments; this.doctors = doctors; this.users = users; this.patients = patients; this.timeSlots = timeSlots; this.events = events;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> patientAppointments(Long userId) {
        patients.patient(userId);
        return appointments.findAllByBookedByIdOrderByAppointmentAtDesc(userId).stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> patientAppointmentsForSubject(Long userId, Long patientId) {
        patients.patient(userId);
        if (!userId.equals(patientId)) patients.dependent(userId, patientId);
        return patientAppointments(userId).stream()
                .filter(item -> item.subject().id().equals(patientId))
                .toList();
    }

    @Transactional
    public AppointmentResponse createForPatient(Long userId, AppointmentCreateRequest request) {
        User owner = patients.patient(userId);
        var patientProfile = patients.profileEntity(userId);
        if (request.appointmentAt().isBefore(LocalDateTime.now(CLINIC_ZONE))) throw new BadRequestException("Дата приёма уже прошла");
        // Lock the doctor's row while validating and creating the appointment. This closes
        // the race where two patients confirm the same slot at the same time.
        DoctorProfile doctor = doctors.findByIdForUpdate(request.doctorId())
                .filter(profile -> profile.isActive() && profile.getUser().isEnabled() && profile.getSpecialty().isActive()
                        && profile.getClinic().isActive() && profile.getClinic().getCity().isActive()
                        && profile.getClinic().getCity().getRegion().isActive())
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        validateBookingFilters(doctor, request);
        Dependent dependent = request.dependentId() == null ? null : patients.dependent(userId, request.dependentId());
        TimeSlot slot = request.timeSlotId() == null
                ? timeSlots.slotForDoctorAt(doctor.getId(), request.appointmentAt())
                : timeSlots.slotForDoctor(request.timeSlotId(), doctor.getId());
        if (!slot.getStartsAt().equals(request.appointmentAt())) throw new BadRequestException("Время не совпадает со свободным слотом");
        if (appointments.existsByDoctorIdAndAppointmentAtAndStatusNot(doctor.getId(), request.appointmentAt(), AppointmentStatus.CANCELLED)) {
            throw new BadRequestException("Это время уже занято. Выберите другое.");
        }
        Appointment appointment = new Appointment();
        appointment.setBookedBy(owner);
        // The care recipient is exactly one of the canonical subjects. The
        // guardian profile remains available through Dependent.patientProfile
        // but must not also be stored as the appointment subject.
        appointment.setPatientProfile(dependent == null ? patientProfile : null);
        appointment.setDependent(dependent);
        appointment.setDoctor(doctor);
        appointment.setTimeSlot(slot);
        appointment.setAppointmentAt(request.appointmentAt());
        appointment.setNote(trimToNull(request.note()));
        appointment.setStatus(AppointmentStatus.BOOKED);
        return AppointmentResponse.from(appointments.save(appointment));
    }

    private void validateBookingFilters(DoctorProfile doctor, AppointmentCreateRequest request) {
        var clinic = doctor.getClinic();
        var city = clinic.getCity();
        if (request.cityId() != null && !city.getId().equals(request.cityId())) throw new BadRequestException("Врач не работает в выбранном городе");
        if (request.specialtyId() != null && !doctor.getSpecialty().getId().equals(request.specialtyId())) throw new BadRequestException("Специальность врача не совпадает с выбранной");
        if (request.clinicId() != null && !clinic.getId().equals(request.clinicId())) throw new BadRequestException("Врач не работает в выбранной клинике");
    }

    @Transactional
    public void cancelForPatient(Long userId, Long appointmentId) {
        cancelForPatient(userId, appointmentId, null);
    }

    @Transactional
    public void cancelForPatient(Long userId, Long appointmentId, String reason) {
        Appointment appointment = appointments.findByIdAndBookedById(appointmentId, userId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
        AppointmentStatus current = appointment.getCanonicalStatus();
        if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED && current != AppointmentStatus.ARRIVED) {
            throw new BadRequestException("На этом этапе запись уже нельзя отменить");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(java.time.Instant.now());
        appointment.setCancelledBy(appointment.getBookedBy());
        appointment.setCancellationReason(trimToNull(reason));
        appointments.save(appointment);
        events.publishEvent(new com.qamqormed.event.AppointmentCancelled(appointment.getId(), appointment.getDoctor().getId(), java.time.Instant.now()));
    }

    @Transactional(readOnly = true)
    public Appointment appointmentForDoctor(Long doctorProfileId, Long appointmentId) {
        return appointments.findByIdAndDoctorId(appointmentId, doctorProfileId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена"));
    }

    @Transactional(readOnly = true)
    public Appointment appointmentForNurse(Long doctorProfileId, Long appointmentId) { return appointmentForDoctor(doctorProfileId, appointmentId); }

    @Transactional(readOnly = true)
    public User user(Long userId) { return users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден")); }

    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
