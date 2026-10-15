package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.event.AppointmentCancelled;
import com.qamqormed.exception.*;
import com.qamqormed.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
public class WaitlistService {
    private final WaitlistEntryRepository entries;
    private final PatientService patients;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final DoctorProfileRepository doctors;
    private final NotificationService notifications;
    private final AppointmentService appointments;
    private final AppointmentRepository appointmentRepository;

    public WaitlistService(WaitlistEntryRepository entries, PatientService patients, ClinicRepository clinics,
                           SpecialtyRepository specialties, DoctorProfileRepository doctors, NotificationService notifications,
                           AppointmentService appointments, AppointmentRepository appointmentRepository) {
        this.entries = entries; this.patients = patients; this.clinics = clinics; this.specialties = specialties;
        this.doctors = doctors; this.notifications = notifications; this.appointments = appointments; this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public WaitlistResponse create(Long userId, WaitlistCreateRequest request) {
        User owner = patients.patient(userId);
        if (request.desiredDateTo().isBefore(request.desiredDateFrom())) throw new BadRequestException("Период листа ожидания указан неверно");
        Dependent dependent = request.dependentId() == null ? null : patients.dependent(userId, request.dependentId());
        Clinic clinic = clinics.findById(request.clinicId()).filter(Clinic::isActive).orElseThrow(() -> new NotFoundException("Клиника не найдена"));
        Specialty specialty = specialties.findById(request.specialtyId()).filter(Specialty::isActive).orElseThrow(() -> new NotFoundException("Специальность не найдена"));
        DoctorProfile doctor = request.doctorId() == null ? null : doctors.findById(request.doctorId()).filter(DoctorProfile::isActive).orElseThrow(() -> new NotFoundException("Врач не найден"));
        if (doctor != null && (!doctor.getClinic().getId().equals(clinic.getId()) || !doctor.getSpecialty().getId().equals(specialty.getId()))) throw new BadRequestException("Врач не соответствует выбранным фильтрам");
        WaitlistEntry item = new WaitlistEntry(); item.setPatientUser(owner); item.setDependent(dependent); item.setClinic(clinic); item.setSpecialty(specialty); item.setDoctor(doctor);
        item.setDesiredDateFrom(request.desiredDateFrom()); item.setDesiredDateTo(request.desiredDateTo()); item.setPreferredTimeFrom(request.preferredTimeFrom()); item.setPreferredTimeTo(request.preferredTimeTo());
        return WaitlistResponse.from(entries.save(item));
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> list(Long userId) { patients.patient(userId); return entries.findAllByPatientUserIdOrderByCreatedAtDesc(userId).stream().map(WaitlistResponse::from).toList(); }

    @Transactional
    public void cancel(Long userId, Long id) {
        WaitlistEntry item = entries.findByIdAndPatientUserId(id, userId).orElseThrow(() -> new NotFoundException("Заявка листа ожидания не найдена"));
        if (item.getStatus() == WaitlistStatus.BOOKED || item.getStatus() == WaitlistStatus.CANCELLED) throw new BadRequestException("Эту заявку уже нельзя отменить");
        item.setStatus(WaitlistStatus.CANCELLED); entries.save(item);
    }

    @Transactional
    public AppointmentResponse book(Long userId, Long id, WaitlistBookRequest request) {
        WaitlistEntry item = entries.findByIdAndPatientUserId(id, userId).orElseThrow(() -> new NotFoundException("Заявка листа ожидания не найдена"));
        if (item.getStatus() != WaitlistStatus.NOTIFIED && item.getStatus() != WaitlistStatus.ACTIVE) throw new BadRequestException("Эта заявка больше не активна");
        if (request.appointmentAt().toLocalDate().isBefore(item.getDesiredDateFrom()) || request.appointmentAt().toLocalDate().isAfter(item.getDesiredDateTo())) throw new BadRequestException("Время не соответствует заявке листа ожидания");
        if (item.getDoctor() != null && !item.getDoctor().getId().equals(request.doctorId())) throw new BadRequestException("Выберите предложенного врача");
        LocalTime time = request.appointmentAt().toLocalTime();
        if (item.getPreferredTimeFrom() != null && time.isBefore(item.getPreferredTimeFrom())) throw new BadRequestException("Время не соответствует заявке листа ожидания");
        if (item.getPreferredTimeTo() != null && time.isAfter(item.getPreferredTimeTo())) throw new BadRequestException("Время не соответствует заявке листа ожидания");
        AppointmentResponse result = appointments.createForPatient(userId, new AppointmentCreateRequest(request.doctorId(), request.appointmentAt(), item.getDependent() == null ? null : item.getDependent().getId(), null, null, item.getSpecialty().getId(), item.getClinic().getId(), request.timeSlotId()));
        item.setStatus(WaitlistStatus.BOOKED); entries.save(item); return result;
    }

    /** Read-only match used by the dev workflow inspector and future slot-offer jobs. */
    @Transactional(readOnly = true)
    public List<WaitlistEntry> matchingEntriesFor(Appointment appointment) {
        LocalDate date = appointment.getAppointmentAt().toLocalDate();
        LocalTime time = appointment.getAppointmentAt().toLocalTime();
        List<WaitlistEntry> candidates = new java.util.ArrayList<>();
        for (WaitlistStatus status : List.of(WaitlistStatus.ACTIVE, WaitlistStatus.NOTIFIED)) {
            candidates.addAll(entries.findAllByStatusAndClinicIdAndSpecialtyIdAndDesiredDateFromLessThanEqualAndDesiredDateToGreaterThanEqual(
                    status, appointment.getDoctor().getClinic().getId(), appointment.getDoctor().getSpecialty().getId(), date, date));
        }
        return candidates.stream().filter(item -> (item.getDoctor() == null || item.getDoctor().getId().equals(appointment.getDoctor().getId()))
                && (item.getPreferredTimeFrom() == null || !time.isBefore(item.getPreferredTimeFrom()))
                && (item.getPreferredTimeTo() == null || !time.isAfter(item.getPreferredTimeTo())))
                .toList();
    }

    @EventListener
    @Transactional
    public void onAppointmentCancelled(AppointmentCancelled event) {
        Appointment appointment = appointmentRepository.findById(event.appointmentId()).orElse(null);
        if (appointment == null) return;
        LocalDate date = appointment.getAppointmentAt().toLocalDate(); LocalTime time = appointment.getAppointmentAt().toLocalTime();
        List<WaitlistEntry> candidates = entries.findAllByStatusAndClinicIdAndSpecialtyIdAndDesiredDateFromLessThanEqualAndDesiredDateToGreaterThanEqual(
                WaitlistStatus.ACTIVE, appointment.getDoctor().getClinic().getId(), appointment.getDoctor().getSpecialty().getId(), date, date);
        candidates.stream().filter(item -> (item.getDoctor() == null || item.getDoctor().getId().equals(appointment.getDoctor().getId()))
                && (item.getPreferredTimeFrom() == null || !time.isBefore(item.getPreferredTimeFrom()))
                && (item.getPreferredTimeTo() == null || !time.isAfter(item.getPreferredTimeTo()))).forEach(item -> {
                    item.setStatus(WaitlistStatus.NOTIFIED); entries.save(item);
                    String doctor = appointment.getDoctor().getUser().getLastName() + " " + appointment.getDoctor().getUser().getFirstName();
                    String url = "/patient/waitlist?waitlistId=" + item.getId() + "&doctorId=" + appointment.getDoctor().getId() + "&appointmentAt=" + appointment.getAppointmentAt();
                    notifications.createIfAbsent(item.getPatientUser(), NotificationType.WAITLIST_SLOT_AVAILABLE, "Освободилось время", "Освободилось время к врачу " + doctor + " — " + date + " в " + time + ".", appointment, url, "waitlist-slot-" + appointment.getId() + "-" + item.getId());
                });
    }
}
