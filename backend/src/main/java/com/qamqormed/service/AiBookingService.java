package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AiBookingService {
    private static final int PROPOSAL_MINUTES = 10;
    private static final int MAX_WAITLIST_DAYS = 31;

    private final AiAppointmentProposalRepository appointmentProposals;
    private final AiWaitlistProposalRepository waitlistProposals;
    private final DoctorProfileRepository doctors;
    private final ClinicRepository clinics;
    private final SpecialtyRepository specialties;
    private final AppointmentRepository appointmentsRepository;
    private final WaitlistEntryRepository waitlistEntries;
    private final TimeSlotService timeSlots;
    private final PatientService patients;
    private final AppointmentService appointments;
    private final WaitlistService waitlist;
    private final ClinicDateService dates;

    public AiBookingService(AiAppointmentProposalRepository appointmentProposals,
                            AiWaitlistProposalRepository waitlistProposals,
                            DoctorProfileRepository doctors, ClinicRepository clinics,
                            SpecialtyRepository specialties, AppointmentRepository appointmentsRepository,
                            WaitlistEntryRepository waitlistEntries, TimeSlotService timeSlots,
                            PatientService patients, AppointmentService appointments,
                            WaitlistService waitlist, ClinicDateService dates) {
        this.appointmentProposals = appointmentProposals;
        this.waitlistProposals = waitlistProposals;
        this.doctors = doctors;
        this.clinics = clinics;
        this.specialties = specialties;
        this.appointmentsRepository = appointmentsRepository;
        this.waitlistEntries = waitlistEntries;
        this.timeSlots = timeSlots;
        this.patients = patients;
        this.appointments = appointments;
        this.waitlist = waitlist;
        this.dates = dates;
    }

    @Transactional
    public AiAppointmentProposalResponse prepareAppointment(Long userId, AiPrepareAppointmentRequest request) {
        User owner = patients.patient(userId);
        DoctorProfile doctor = activeDoctor(request.doctorId());
        Dependent dependent = request.dependentId() == null ? null : patients.dependent(userId, request.dependentId());
        TimeSlot slot = timeSlots.slotForDoctor(request.timeSlotId(), doctor.getId());
        LocalDateTime now = dates.now();
        if (!slot.getStartsAt().isAfter(now)) throw new BadRequestException("Выбранное время уже прошло");
        if (appointmentsRepository.existsByDoctorIdAndAppointmentAtAndStatusNot(doctor.getId(), slot.getStartsAt(), AppointmentStatus.CANCELLED)) {
            throw new ConflictException("Выбранное время уже занято. Выберите другое.");
        }

        AiAppointmentProposal proposal = new AiAppointmentProposal();
        proposal.setOwner(owner);
        proposal.setDoctor(doctor);
        proposal.setDependent(dependent);
        proposal.setTimeSlot(slot);
        proposal.setAppointmentAt(slot.getStartsAt());
        proposal.setExpiresAt(dates.nowInstant().plus(PROPOSAL_MINUTES, ChronoUnit.MINUTES));
        return appointmentResponse(appointmentProposals.save(proposal), null);
    }

    @Transactional
    public AiAppointmentProposalResponse confirmAppointment(Long userId, Long proposalId) {
        patients.patient(userId);
        AiAppointmentProposal proposal = appointmentProposals.findByIdAndOwnerIdForUpdate(proposalId, userId)
                .orElseThrow(() -> new NotFoundException("Предложение записи не найдено"));
        if (proposal.getStatus() == AiProposalStatus.CONFIRMED && proposal.getConfirmedAppointmentId() != null) {
            Appointment appointment = appointmentsRepository.findByIdAndBookedById(proposal.getConfirmedAppointmentId(), userId)
                    .orElseThrow(() -> new NotFoundException("Подтверждённая запись не найдена"));
            return appointmentResponse(proposal, AppointmentResponse.from(appointment));
        }
        ensurePrepared(proposal);
        AppointmentResponse result = appointments.createForPatient(userId, new AppointmentCreateRequest(
                proposal.getDoctor().getId(), proposal.getAppointmentAt(),
                proposal.getDependent() == null ? null : proposal.getDependent().getId(),
                null, proposal.getDoctor().getClinic().getCity().getId(),
                proposal.getDoctor().getSpecialty().getId(), proposal.getDoctor().getClinic().getId(),
                proposal.getTimeSlot().getId()));
        proposal.setStatus(AiProposalStatus.CONFIRMED);
        proposal.setConfirmedAppointmentId(result.id());
        appointmentProposals.save(proposal);
        return appointmentResponse(proposal, result);
    }

    @Transactional
    public void cancelAppointment(Long userId, Long proposalId) {
        patients.patient(userId);
        AiAppointmentProposal proposal = appointmentProposals.findByIdAndOwnerIdForUpdate(proposalId, userId)
                .orElseThrow(() -> new NotFoundException("Предложение записи не найдено"));
        if (proposal.getStatus() == AiProposalStatus.CONFIRMED) throw new BadRequestException("Подтверждённую запись отменяйте в разделе записей");
        if (proposal.getStatus() == AiProposalStatus.PREPARED) {
            proposal.setStatus(AiProposalStatus.CANCELLED);
            appointmentProposals.save(proposal);
        }
    }

    @Transactional
    public AiWaitlistProposalResponse prepareWaitlist(Long userId, AiPrepareWaitlistRequest request) {
        User owner = patients.patient(userId);
        if (request.desiredDateTo().isBefore(request.desiredDateFrom())) throw new BadRequestException("Период листа ожидания указан неверно");
        LocalDate today = dates.today();
        if (request.desiredDateFrom().isBefore(today)) throw new BadRequestException("Дата листа ожидания уже прошла");
        if (request.desiredDateTo().isAfter(request.desiredDateFrom().plusDays(MAX_WAITLIST_DAYS))) throw new BadRequestException("Период листа ожидания слишком большой");
        if (request.preferredTimeFrom() != null && request.preferredTimeTo() != null && request.preferredTimeTo().isBefore(request.preferredTimeFrom())) {
            throw new BadRequestException("Временной диапазон указан неверно");
        }
        Dependent dependent = request.dependentId() == null ? null : patients.dependent(userId, request.dependentId());
        Clinic clinic = activeClinic(request.clinicId());
        Specialty specialty = activeSpecialty(request.specialtyId());
        DoctorProfile doctor = request.doctorId() == null ? null : activeDoctor(request.doctorId());
        if (doctor != null && (!doctor.getClinic().getId().equals(clinic.getId()) || !doctor.getSpecialty().getId().equals(specialty.getId()))) {
            throw new BadRequestException("Врач не соответствует выбранным фильтрам");
        }

        AiWaitlistProposal proposal = new AiWaitlistProposal();
        proposal.setOwner(owner);
        proposal.setDependent(dependent);
        proposal.setClinic(clinic);
        proposal.setSpecialty(specialty);
        proposal.setDoctor(doctor);
        proposal.setDesiredDateFrom(request.desiredDateFrom());
        proposal.setDesiredDateTo(request.desiredDateTo());
        proposal.setPreferredTimeFrom(request.preferredTimeFrom());
        proposal.setPreferredTimeTo(request.preferredTimeTo());
        proposal.setExpiresAt(dates.nowInstant().plus(PROPOSAL_MINUTES, ChronoUnit.MINUTES));
        return waitlistResponse(waitlistProposals.save(proposal), null);
    }

    @Transactional
    public AiWaitlistProposalResponse confirmWaitlist(Long userId, Long proposalId) {
        patients.patient(userId);
        AiWaitlistProposal proposal = waitlistProposals.findByIdAndOwnerIdForUpdate(proposalId, userId)
                .orElseThrow(() -> new NotFoundException("Предложение листа ожидания не найдено"));
        if (proposal.getStatus() == AiProposalStatus.CONFIRMED && proposal.getConfirmedWaitlistId() != null) {
            WaitlistResponse existing = waitlistEntries.findByIdAndPatientUserId(proposal.getConfirmedWaitlistId(), userId)
                    .map(WaitlistResponse::from)
                    .orElseThrow(() -> new NotFoundException("Заявка листа ожидания не найдена"));
            return waitlistResponse(proposal, existing);
        }
        ensurePrepared(proposal);
        WaitlistResponse result = waitlist.create(userId, new WaitlistCreateRequest(
                proposal.getDependent() == null ? null : proposal.getDependent().getId(),
                proposal.getClinic().getId(), proposal.getSpecialty().getId(),
                proposal.getDoctor() == null ? null : proposal.getDoctor().getId(),
                proposal.getDesiredDateFrom(), proposal.getDesiredDateTo(),
                proposal.getPreferredTimeFrom(), proposal.getPreferredTimeTo()));
        proposal.setStatus(AiProposalStatus.CONFIRMED);
        proposal.setConfirmedWaitlistId(result.id());
        waitlistProposals.save(proposal);
        return waitlistResponse(proposal, result);
    }

    @Transactional
    public void cancelWaitlist(Long userId, Long proposalId) {
        patients.patient(userId);
        AiWaitlistProposal proposal = waitlistProposals.findByIdAndOwnerIdForUpdate(proposalId, userId)
                .orElseThrow(() -> new NotFoundException("Предложение листа ожидания не найдено"));
        if (proposal.getStatus() == AiProposalStatus.CONFIRMED) throw new BadRequestException("Подтверждённую заявку отменяйте в разделе листа ожидания");
        if (proposal.getStatus() == AiProposalStatus.PREPARED) {
            proposal.setStatus(AiProposalStatus.CANCELLED);
            waitlistProposals.save(proposal);
        }
    }

    private DoctorProfile activeDoctor(Long id) {
        return doctors.findById(id)
                .filter(value -> value.isActive() && value.getUser().isEnabled() && value.getSpecialty().isActive()
                        && value.getClinic().isActive() && value.getClinic().getCity().isActive()
                        && value.getClinic().getCity().getRegion().isActive())
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
    }

    private Clinic activeClinic(Long id) {
        return clinics.findById(id)
                .filter(value -> value.isActive() && value.getCity().isActive() && value.getCity().getRegion().isActive())
                .orElseThrow(() -> new NotFoundException("Клиника не найдена"));
    }

    private Specialty activeSpecialty(Long id) {
        return specialties.findById(id).filter(Specialty::isActive)
                .orElseThrow(() -> new NotFoundException("Специальность не найдена"));
    }

    private void ensurePrepared(AiAppointmentProposal proposal) {
        if (proposal.getStatus() != AiProposalStatus.PREPARED) throw new BadRequestException("Предложение записи больше недоступно");
        if (proposal.getExpiresAt().isBefore(dates.nowInstant())) {
            proposal.setStatus(AiProposalStatus.EXPIRED);
            appointmentProposals.save(proposal);
            throw new BadRequestException("Срок предложения записи истёк");
        }
    }

    private void ensurePrepared(AiWaitlistProposal proposal) {
        if (proposal.getStatus() != AiProposalStatus.PREPARED) throw new BadRequestException("Предложение листа ожидания больше недоступно");
        if (proposal.getExpiresAt().isBefore(dates.nowInstant())) {
            proposal.setStatus(AiProposalStatus.EXPIRED);
            waitlistProposals.save(proposal);
            throw new BadRequestException("Срок предложения листа ожидания истёк");
        }
    }

    private AiAppointmentProposalResponse appointmentResponse(AiAppointmentProposal proposal, AppointmentResponse appointment) {
        DoctorProfile doctor = proposal.getDoctor();
        Clinic clinic = doctor.getClinic();
        return new AiAppointmentProposalResponse(proposal.getId(), proposal.getStatus().name(), proposal.getExpiresAt(),
                subject(proposal.getOwner(), proposal.getDependent()),
                new AiAppointmentProposalResponse.DoctorCard(doctor.getId(), fullName(doctor.getUser()), doctor.getSpecialty().getName()),
                new AiAppointmentProposalResponse.ClinicCard(clinic.getId(), clinic.getName(), clinic.getCity().getName(), clinic.getCity().getRegion().getName()),
                proposal.getAppointmentAt(), proposal.getTimeSlot().getId(), appointment);
    }

    private AiWaitlistProposalResponse waitlistResponse(AiWaitlistProposal proposal, WaitlistResponse waitlist) {
        Clinic clinic = proposal.getClinic();
        return new AiWaitlistProposalResponse(proposal.getId(), proposal.getStatus().name(), proposal.getExpiresAt(),
                subject(proposal.getOwner(), proposal.getDependent()),
                new AiAppointmentProposalResponse.ClinicCard(clinic.getId(), clinic.getName(), clinic.getCity().getName(), clinic.getCity().getRegion().getName()),
                proposal.getSpecialty().getId(), proposal.getSpecialty().getName(),
                proposal.getDoctor() == null ? null : proposal.getDoctor().getId(), proposal.getDesiredDateFrom(), proposal.getDesiredDateTo(),
                proposal.getPreferredTimeFrom(), proposal.getPreferredTimeTo(), waitlist);
    }

    private AiAppointmentProposalResponse.SubjectCard subject(User owner, Dependent dependent) {
        if (dependent != null) return new AiAppointmentProposalResponse.SubjectCard(dependent.getId(), "DEPENDENT", fullName(dependent.getLastName(), dependent.getFirstName(), dependent.getMiddleName()));
        return new AiAppointmentProposalResponse.SubjectCard(owner.getId(), "SELF", fullName(owner));
    }

    private String fullName(User user) { return fullName(user.getLastName(), user.getFirstName(), user.getMiddleName()); }

    private String fullName(String lastName, String firstName, String middleName) {
        return String.join(" ", lastName, firstName, middleName == null ? "" : middleName).trim().replaceAll(" +", " ");
    }
}
