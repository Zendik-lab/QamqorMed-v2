package com.qamqormed.service;

import com.qamqormed.dto.DevNotificationWorkflowResponse;
import com.qamqormed.entity.*;
import com.qamqormed.exception.ForbiddenException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Dev-only adapter around the production services. It owns no booking rules: every
 * mutation is delegated to AppointmentScheduler or ClinicalWorkflowService.
 */
@Service
@Profile("dev")
public class DevNotificationWorkflowService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");
    private static final String DEMO_PHONE_PREFIX = "+770000000";
    private final AppointmentRepository appointments;
    private final NotificationRepository notifications;
    private final AppointmentStatusHistoryRepository history;
    private final WaitlistService waitlists;
    private final DoctorNurseAssignmentRepository assignments;
    private final AppointmentScheduler scheduler;
    private final ClinicalWorkflowService workflow;

    public DevNotificationWorkflowService(AppointmentRepository appointments, NotificationRepository notifications,
                                          AppointmentStatusHistoryRepository history, WaitlistService waitlists,
                                          DoctorNurseAssignmentRepository assignments, AppointmentScheduler scheduler,
                                          ClinicalWorkflowService workflow) {
        this.appointments = appointments;
        this.notifications = notifications;
        this.history = history;
        this.waitlists = waitlists;
        this.assignments = assignments;
        this.scheduler = scheduler;
        this.workflow = workflow;
    }

    @Transactional(readOnly = true)
    public List<DevNotificationWorkflowResponse.AppointmentView> search(Long appointmentId, String patient,
                                                                          String doctor, String clinic) {
        return appointments.findAll().stream().filter(this::isDemoAppointment)
                .filter(item -> appointmentId == null || appointmentId.equals(item.getId()))
                .filter(item -> contains(patientText(item), patient))
                .filter(item -> contains(fullName(item.getDoctor().getUser()), doctor))
                .filter(item -> contains(item.getDoctor().getClinic().getName(), clinic))
                .sorted(Comparator.comparing(Appointment::getAppointmentAt).reversed())
                .map(this::appointmentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<com.qamqormed.dto.DevNotificationWorkflowResponse.AppointmentView> searchAll(
            Long appointmentId, String patient, String doctor, String clinic,
            Long regionId, Long cityId, Long clinicId, Long doctorId,
            LocalDate appointmentDate, String status, String confirmationStatus,
            int page, int size, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), parseSort(sort));
        return appointments.findAll(allAppointmentsSpecification(appointmentId, patient, doctor, clinic,
                        regionId, cityId, clinicId, doctorId, appointmentDate, status, confirmationStatus), pageable)
                .map(this::appointmentView);
    }

    @Transactional(readOnly = true)
    public DevNotificationWorkflowResponse inspect(Long appointmentId) {
        return response(anyAppointment(appointmentId), null, null);
    }

    @Transactional
    public DevNotificationWorkflowResponse reminder24h(Long appointmentId) { return reminder(appointmentId, true); }

    @Transactional
    public DevNotificationWorkflowResponse reminder2h(Long appointmentId) { return reminder(appointmentId, false); }

    @Transactional
    public DevNotificationWorkflowResponse confirmationRequest(Long appointmentId) {
        Appointment appointment = demoAppointment(appointmentId);
        Instant evaluatedAt = appointment.getAppointmentAt().atZone(CLINIC_ZONE).minusHours(24).toInstant();
        AppointmentScheduler.SchedulerRunResult result = scheduler.processAt(evaluatedAt);
        boolean found = notifications.findAllByRelatedAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .anyMatch(item -> item.getType() == NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED);
        String status = found ? "PASS" : "FAIL";
        String message = found ? "Запрос подтверждения посещения создан или уже существует"
                : "Scheduler не создал запрос подтверждения: запись не находится в статусе, допускающем подтверждение";
        return response(appointment, result, action("confirmation-request", status, message,
                List.of("Тестовое время scheduler: " + evaluatedAt, "Тип уведомления: APPOINTMENT_CONFIRMATION_REQUIRED")));
    }

    @Transactional
    public DevNotificationWorkflowResponse simulateNoShow(Long appointmentId) {
        Appointment appointment = demoAppointment(appointmentId);
        if (appointment.getCanonicalStatus() == AppointmentStatus.NO_SHOW) {
            return response(appointment, null, action("no-show", "PASS", "Запись уже имеет статус NO_SHOW",
                    List.of("Повторный запуск безопасен и не создаёт новую историю")));
        }
        Instant evaluatedAt = appointment.getAppointmentAt().atZone(CLINIC_ZONE).plusMinutes(31).toInstant();
        AppointmentScheduler.SchedulerRunResult result = scheduler.processAt(evaluatedAt);
        Appointment refreshed = demoAppointment(appointmentId);
        boolean passed = refreshed.getCanonicalStatus() == AppointmentStatus.NO_SHOW && refreshed.getNoShowAt() != null;
        return response(refreshed, result, action("no-show", passed ? "PASS" : "FAIL",
                passed ? "Grace period пройден, scheduler отметил запись как NO_SHOW"
                        : "Scheduler не перевёл запись в NO_SHOW; проверьте статус и время записи",
                List.of("Тестовое время scheduler: " + evaluatedAt, "Grace period: 30 минут")));
    }

    @Transactional
    public DevNotificationWorkflowResponse markArrived(Long appointmentId) {
        Appointment appointment = demoAppointment(appointmentId);
        AppointmentStatus current = appointment.getCanonicalStatus();
        if (current == AppointmentStatus.ARRIVED || current == AppointmentStatus.PREPARING || current == AppointmentStatus.READY) {
            return response(appointment, null, action("arrived", "PASS", "Пациент уже отмечен прибывшим",
                    List.of("Текущий статус: " + current)));
        }
        if (current != AppointmentStatus.BOOKED && current != AppointmentStatus.CONFIRMED) {
            return response(appointment, null, action("arrived", "FAIL", "Нельзя отметить прибытие из текущего статуса " + current,
                    List.of("Ожидается BOOKED или CONFIRMED")));
        }
        var assignment = assignments.findActiveByDoctorId(appointment.getDoctor().getId()).orElse(null);
        if (assignment == null || assignment.getNurse() == null || assignment.getNurse().getUser() == null) {
            return response(appointment, null, action("arrived", "FAIL", "Для DEMO-врача не найдена активная медсестра",
                    List.of("Прибытие выполняется через ClinicalWorkflowService.checkInByNurse")));
        }
        workflow.checkInByNurse(assignment.getNurse().getUser().getId(), appointmentId);
        Appointment refreshed = demoAppointment(appointmentId);
        boolean passed = refreshed.getCanonicalStatus() == AppointmentStatus.ARRIVED && refreshed.getArrivedAt() != null;
        return response(refreshed, null, action("arrived", passed ? "PASS" : "FAIL",
                passed ? "ClinicalWorkflowService отметил пациента прибывшим"
                        : "Сервис workflow не изменил статус на ARRIVED",
                List.of("Использована DEMO-медсестра: " + fullName(assignment.getNurse().getUser()))));
    }

    @Transactional(readOnly = true)
    public DevNotificationWorkflowResponse checkWaitlist(Long appointmentId) {
        Appointment appointment = demoAppointment(appointmentId);
        List<WaitlistEntry> matches = waitlists.matchingEntriesFor(appointment);
        boolean released = appointment.getCanonicalStatus() == AppointmentStatus.CANCELLED;
        String message = released
                ? "Слот освобождён; matching Waitlist-заявки проверены"
                : "Слот ещё не освобождён: для текущей записи нужен статус CANCELLED";
        return response(appointment, null, action("waitlist", released ? "PASS" : "FAIL", message,
                List.of("Matching Waitlist-заявок: " + matches.size(),
                        "NO_SHOW не считается освобождением слота в текущей бизнес-логике")));
    }

    private DevNotificationWorkflowResponse reminder(Long appointmentId, boolean reminder24h) {
        Appointment appointment = demoAppointment(appointmentId);
        Instant evaluatedAt = appointment.getAppointmentAt().atZone(CLINIC_ZONE)
                .minusHours(reminder24h ? 24 : 2).toInstant();
        AppointmentScheduler.SchedulerRunResult result = scheduler.processAt(evaluatedAt);
        NotificationType expected = reminder24h ? NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED : NotificationType.APPOINTMENT_REMINDER;
        boolean found = notifications.findAllByRelatedAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .anyMatch(item -> item.getType() == expected);
        String actionName = reminder24h ? "reminder-24h" : "reminder-2h";
        return response(appointment, result, action(actionName, found ? "PASS" : "FAIL",
                found ? "Scheduler создал уведомление или подтвердил его идемпотентное наличие"
                        : "Scheduler не создал ожидаемое уведомление; проверьте статус и время записи",
                List.of("Тестовое время scheduler: " + evaluatedAt, "Ожидаемый тип: " + expected)));
    }

    private DevNotificationWorkflowResponse response(Appointment appointment, AppointmentScheduler.SchedulerRunResult schedulerResult,
                                                      DevNotificationWorkflowResponse.ActionResult action) {
        List<DevNotificationWorkflowResponse.StatusHistoryView> statusHistory = history.findAllByAppointmentIdOrderByChangedAtAsc(appointment.getId()).stream()
                .map(this::historyView).toList();
        List<DevNotificationWorkflowResponse.NotificationView> notificationViews = notifications.findAllByRelatedAppointmentIdOrderByCreatedAtDesc(appointment.getId()).stream()
                .map(item -> new DevNotificationWorkflowResponse.NotificationView(item.getId(), item.getType().name(), item.getTitle(),
                        item.getMessage(), item.getCreatedAt(), item.getReadAt(), item.isRead(), item.getEventKey())).toList();
        List<DevNotificationWorkflowResponse.WaitlistView> waitlistViews = waitlists.matchingEntriesFor(appointment).stream()
                .map(this::waitlistView).toList();
        DevNotificationWorkflowResponse.SchedulerView schedulerView = schedulerResult == null ? null
                : new DevNotificationWorkflowResponse.SchedulerView(schedulerResult.evaluatedAt(), schedulerResult.reminder24hAppointmentIds(),
                schedulerResult.reminder2hAppointmentIds(), schedulerResult.noShowAppointmentIds());
        return new DevNotificationWorkflowResponse(appointmentView(appointment), statusHistory, notificationViews, waitlistViews, schedulerView, action);
    }

    private Appointment demoAppointment(Long id) {
        Appointment appointment = anyAppointment(id);
        if (!isDemoAppointment(appointment)) throw new ForbiddenException("Действие доступно только для DEMO-записей");
        return appointment;
    }

    private Appointment anyAppointment(Long id) {
        return appointments.findById(id).orElseThrow(() -> new NotFoundException("Запись не найдена"));
    }

    private boolean isDemoAppointment(Appointment appointment) {
        return appointment != null && appointment.getDoctor() != null && appointment.getDoctor().isDemo()
                && appointment.getBookedBy() != null && appointment.getBookedBy().getPhone() != null
                && appointment.getBookedBy().getPhone().startsWith(DEMO_PHONE_PREFIX)
                && appointment.getNote() != null && appointment.getNote().startsWith("DEV_");
    }

    private DevNotificationWorkflowResponse.AppointmentView appointmentView(Appointment item) {
        String rawStatus = item.getStatus() == null ? null : item.getStatus().name();
        String canonicalStatus = item.getCanonicalStatus() == null ? null : item.getCanonicalStatus().name();
        var doctor = item.getDoctor();
        var clinic = doctor == null ? null : doctor.getClinic();
        var city = clinic == null ? null : clinic.getCity();
        var region = city == null ? null : city.getRegion();
        return new DevNotificationWorkflowResponse.AppointmentView(item.getId(), item.getBookedBy().getId(), fullName(item.getBookedBy()),
                recipientName(item), item.getDependent() == null ? null : dependentName(item),
                item.getDependent() == null ? null : item.getDependent().getId(), doctor == null ? null : doctor.getId(),
                doctor == null || doctor.getUser() == null ? null : doctor.getUser().getId(),
                doctor == null || doctor.getUser() == null ? null : fullName(doctor.getUser()),
                doctor == null || doctor.getSpecialty() == null ? null : doctor.getSpecialty().getName(),
                clinic == null ? null : clinic.getId(), clinic == null ? null : clinic.getName(),
                city == null ? null : city.getName(), region == null ? null : region.getName(), item.getAppointmentAt(),
                rawStatus, canonicalStatus, item.getConfirmationStatus() == null ? null : item.getConfirmationStatus().name(),
                item.getCreatedAt(), item.getConfirmedAt(), item.getArrivedAt(), item.getNoShowAt(),
                item.getTimeSlot() == null ? null : item.getTimeSlot().getId(),
                canonicalStatus == null || AppointmentStatus.CANCELLED.name().equals(canonicalStatus), isDemoAppointment(item));
    }

    private DevNotificationWorkflowResponse.StatusHistoryView historyView(AppointmentStatusHistory item) {
        return new DevNotificationWorkflowResponse.StatusHistoryView(item.getId(), item.getOldStatus() == null ? null : item.getOldStatus().name(),
                item.getNewStatus().name(), item.getChangedAt(), item.getChangedBy() == null ? null : item.getChangedBy().getId(),
                item.getChangedBy() == null ? null : fullName(item.getChangedBy()), item.getReason());
    }

    private DevNotificationWorkflowResponse.WaitlistView waitlistView(WaitlistEntry item) {
        String patient = item.getDependent() == null ? fullName(item.getPatientUser()) : fullName(item.getDependent().getLastName(), item.getDependent().getFirstName(), item.getDependent().getMiddleName());
        return new DevNotificationWorkflowResponse.WaitlistView(item.getId(), item.getPatientUser().getId(), patient,
                item.getDependent() == null ? null : item.getDependent().getId(), item.getDoctor() == null ? null : item.getDoctor().getId(),
                item.getStatus().name(), item.getDesiredDateFrom(), item.getDesiredDateTo(), item.getPreferredTimeFrom(), item.getPreferredTimeTo());
    }

    private DevNotificationWorkflowResponse.ActionResult action(String name, String status, String message, List<String> details) {
        return new DevNotificationWorkflowResponse.ActionResult(name, status, message, details, Instant.now());
    }

    private String patientText(Appointment item) {
        return recipientName(item);
    }

    private String recipientName(Appointment item) {
        if (item.getDependent() != null) return fullName(item.getDependent().getLastName(), item.getDependent().getFirstName(), item.getDependent().getMiddleName());
        if (item.getPatientProfile() != null) return fullName(item.getPatientProfile().getLastName(), item.getPatientProfile().getFirstName(), item.getPatientProfile().getMiddleName());
        return fullName(item.getBookedBy());
    }

    private String dependentName(Appointment item) {
        return item.getDependent() == null ? null : fullName(item.getDependent().getLastName(), item.getDependent().getFirstName(), item.getDependent().getMiddleName());
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "createdAt");
        String[] parts = sort.split(",", 2);
        String field = switch (parts[0]) {
            case "createdAt", "appointmentAt", "updatedAt", "id" -> parts[0];
            default -> "createdAt";
        };
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private Specification<Appointment> allAppointmentsSpecification(Long appointmentId, String patient, String doctor,
                                                                     String clinic, Long regionId, Long cityId, Long clinicId,
                                                                     Long doctorId, LocalDate appointmentDate, String status,
                                                                     String confirmationStatus) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Appointment, User> bookedBy = root.join("bookedBy", JoinType.INNER);
            Join<Appointment, Dependent> dependent = root.join("dependent", JoinType.LEFT);
            Join<Appointment, PatientProfile> patientProfile = root.join("patientProfile", JoinType.LEFT);
            Join<Appointment, DoctorProfile> doctorProfile = root.join("doctor", JoinType.INNER);
            Join<DoctorProfile, User> doctorUser = doctorProfile.join("user", JoinType.INNER);
            Join<DoctorProfile, Clinic> doctorClinic = doctorProfile.join("clinic", JoinType.INNER);
            Join<Clinic, City> doctorCity = doctorClinic.join("city", JoinType.INNER);
            Join<City, Region> doctorRegion = doctorCity.join("region", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            if (appointmentId != null) predicates.add(cb.equal(root.get("id"), appointmentId));
            addNameFilter(predicates, cb, patient, bookedBy, dependent, patientProfile, "patient");
            addTextFilter(predicates, cb, doctor, doctorUser, List.of("firstName", "lastName", "middleName"));
            addTextFilter(predicates, cb, clinic, doctorClinic, List.of("name"));
            if (regionId != null) predicates.add(cb.equal(doctorRegion.get("id"), regionId));
            if (cityId != null) predicates.add(cb.equal(doctorCity.get("id"), cityId));
            if (clinicId != null) predicates.add(cb.equal(doctorClinic.get("id"), clinicId));
            if (doctorId != null) predicates.add(cb.equal(doctorProfile.get("id"), doctorId));
            if (appointmentDate != null) {
                LocalDateTime from = appointmentDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("appointmentAt"), from));
                predicates.add(cb.lessThan(root.get("appointmentAt"), from.plusDays(1)));
            }
            addStatusFilter(predicates, cb, root, status, "status", AppointmentStatus.class);
            addStatusFilter(predicates, cb, root, confirmationStatus, "confirmationStatus", AppointmentConfirmationStatus.class);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addNameFilter(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb, String value,
                               Join<Appointment, User> bookedBy, Join<Appointment, Dependent> dependent,
                               Join<Appointment, PatientProfile> patientProfile, String ignored) {
        if (value == null || value.isBlank()) return;
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.or(
                cb.like(cb.lower(bookedBy.get("firstName")), pattern), cb.like(cb.lower(bookedBy.get("lastName")), pattern), cb.like(cb.lower(bookedBy.get("middleName")), pattern),
                cb.like(cb.lower(dependent.get("firstName")), pattern), cb.like(cb.lower(dependent.get("lastName")), pattern), cb.like(cb.lower(dependent.get("middleName")), pattern),
                cb.like(cb.lower(patientProfile.get("firstName")), pattern), cb.like(cb.lower(patientProfile.get("lastName")), pattern), cb.like(cb.lower(patientProfile.get("middleName")), pattern)
        ));
    }

    private void addTextFilter(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb, String value,
                               jakarta.persistence.criteria.From<?, ?> from, List<String> fields) {
        if (value == null || value.isBlank()) return;
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.or(fields.stream().map(field -> cb.like(cb.lower(from.get(field)), pattern)).toArray(Predicate[]::new)));
    }

    private <E extends Enum<E>> void addStatusFilter(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                                     jakarta.persistence.criteria.Root<Appointment> root, String value,
                                                     String field, Class<E> type) {
        if (value == null || value.isBlank()) return;
        try {
            predicates.add(cb.equal(root.get(field), Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT))));
        } catch (IllegalArgumentException ignored) {
            predicates.add(cb.disjunction());
        }
    }

    private boolean contains(String value, String query) {
        return query == null || query.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)));
    }

    private String fullName(User user) { return fullName(user.getLastName(), user.getFirstName(), user.getMiddleName()); }
    private String fullName(String last, String first, String middle) { return String.join(" ", last == null ? "" : last, first == null ? "" : first, middle == null ? "" : middle).trim().replaceAll(" +", " "); }
}
