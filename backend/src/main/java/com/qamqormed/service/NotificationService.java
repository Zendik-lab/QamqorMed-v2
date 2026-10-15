package com.qamqormed.service;

import com.qamqormed.entity.*;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.NotificationRepository;
import com.qamqormed.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications; this.users = users;
    }

    @Transactional(readOnly = true)
    public List<com.qamqormed.dto.NotificationResponse> list(Long userId) {
        return notifications.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(com.qamqormed.dto.NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) { return notifications.countByUserIdAndReadFalse(userId); }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification value = notifications.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Уведомление не найдено"));
        if (!value.isRead()) { value.setRead(true); value.setReadAt(Instant.now()); notifications.save(value); }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.findAllByUserIdOrderByCreatedAtDesc(userId).stream().filter(value -> !value.isRead()).forEach(value -> {
            value.setRead(true); value.setReadAt(Instant.now());
        });
    }

    @Transactional
    public Notification createIfAbsent(User user, NotificationType type, String title, String message,
                                       Appointment appointment, String actionUrl, String eventKey) {
        if (notifications.existsByEventKey(eventKey)) return null;
        Notification value = new Notification();
        value.setUser(user); value.setType(type); value.setTitle(title); value.setMessage(message);
        value.setRelatedAppointment(appointment); value.setActionUrl(actionUrl); value.setEventKey(eventKey);
        return notifications.save(value);
    }

    @Transactional
    public void appointmentCreated(Appointment appointment) {
        createIfAbsent(appointment.getBookedBy(), NotificationType.APPOINTMENT_CREATED, "Вы записаны на приём",
                appointmentMessage(appointment, "Запись подтверждена"), appointment, "/patient/my-appointments", "appointment-created-" + appointment.getId());
    }

    @Transactional
    public void appointmentConfirmed(Appointment appointment) {
        createIfAbsent(appointment.getBookedBy(), NotificationType.APPOINTMENT_CONFIRMED, "Приём подтверждён",
                appointmentMessage(appointment, "Вы подтвердили визит"), appointment, "/patient/my-appointments", "appointment-confirmed-" + appointment.getId());
    }

    @Transactional
    public void appointmentCancelled(Appointment appointment) {
        createIfAbsent(appointment.getBookedBy(), NotificationType.APPOINTMENT_CANCELLED, "Запись отменена",
                appointmentMessage(appointment, "Запись отменена"), appointment, "/patient/my-appointments", "appointment-cancelled-" + appointment.getId() + "-" + appointment.getUpdatedAt());
    }

    public String appointmentMessage(Appointment appointment, String prefix) {
        String patient = appointment.getDependent() == null ? null : fullName(appointment.getDependent().getLastName(), appointment.getDependent().getFirstName(), appointment.getDependent().getMiddleName());
        String doctor = fullName(appointment.getDoctor().getUser().getLastName(), appointment.getDoctor().getUser().getFirstName(), appointment.getDoctor().getUser().getMiddleName());
        StringBuilder result = new StringBuilder(prefix).append(". ");
        if (patient != null) result.append("Пациент: ").append(patient).append(". ");
        result.append("Врач: ").append(doctor).append(", ").append(appointment.getDoctor().getSpecialty().getName()).append(". ")
                .append(appointment.getDoctor().getClinic().getName()).append(". ")
                .append(appointment.getAppointmentAt().format(DATE)).append(", ").append(appointment.getAppointmentAt().format(TIME)).append(".");
        return result.toString();
    }

    @Transactional
    public void reminder(Appointment appointment, NotificationType type, String eventKey) {
        String title = type == NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED ? "Подтвердите визит" : "Напоминание о приёме";
        String text = type == NotificationType.APPOINTMENT_CONFIRMATION_REQUIRED
                ? "Подтвердите, пожалуйста, что вы придёте на приём " + appointment.getAppointmentAt().format(DATE) + " в " + appointment.getAppointmentAt().format(TIME) + "."
                : "Напоминаем о приёме " + appointment.getAppointmentAt().format(DATE) + " в " + appointment.getAppointmentAt().format(TIME) + ".";
        createIfAbsent(appointment.getBookedBy(), type, title, text, appointment, "/patient/my-appointments", eventKey);
    }

    @Transactional
    public void medical(User patient, NotificationType type, String title, String text, Appointment appointment, String actionUrl, String eventKey) {
        createIfAbsent(patient, type, title, subjectPrefix(appointment) + text, appointment, actionUrl, eventKey);
    }

    private String subjectPrefix(Appointment appointment) {
        if (appointment.getDependent() == null) return "";
        return "Для пациента: " + fullName(appointment.getDependent().getLastName(), appointment.getDependent().getFirstName(), appointment.getDependent().getMiddleName()) + ". ";
    }
    private String fullName(String last, String first, String middle) { return String.join(" ", last, first, middle == null ? "" : middle).trim().replaceAll(" +", " "); }
}
