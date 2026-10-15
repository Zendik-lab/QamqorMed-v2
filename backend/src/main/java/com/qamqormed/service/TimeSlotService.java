package com.qamqormed.service;

import com.qamqormed.dto.TimeSlotResponse;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.TimeSlot;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeSlotService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");
    private final TimeSlotRepository slots;
    private final DoctorProfileRepository doctors;
    private final ScheduleService schedules;

    public TimeSlotService(TimeSlotRepository slots, DoctorProfileRepository doctors, ScheduleService schedules) {
        this.slots = slots; this.doctors = doctors; this.schedules = schedules;
    }

    @Transactional
    public List<TimeSlotResponse> available(Long doctorId, LocalDate from, LocalDate to) {
        DoctorProfile doctor = doctors.findById(doctorId)
                .filter(DoctorProfile::isAvailableForBooking)
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        LocalDate start = from == null || from.isBefore(today) ? today : from;
        LocalDate end = to == null ? start.plusDays(14) : to;
        if (end.isAfter(start.plusDays(31))) end = start.plusDays(31);
        var schedule = schedules.scheduleForDoctor(doctor);
        ensureSlots(doctor, schedule, start, end);
        LocalDateTime fromAt = start.equals(today) ? LocalDateTime.now(CLINIC_ZONE) : start.atStartOfDay();
        return slots.findAvailable(doctorId, fromAt, end.plusDays(1).atStartOfDay()).stream()
                .filter(slot -> schedules.isBookable(schedule, slot.getStartsAt()))
                .map(TimeSlotResponse::from).toList();
    }

    @Transactional
    public TimeSlot slotForDoctor(Long slotId, Long doctorId) {
        TimeSlot slot = slots.findById(slotId)
                .filter(value -> value.isActive() && value.getDoctor().getId().equals(doctorId)
                        && value.getDoctor().isAvailableForBooking())
                .orElseThrow(() -> new NotFoundException("Время приёма не найдено"));
        if (!schedules.isBookable(schedules.scheduleForDoctor(slot.getDoctor()), slot.getStartsAt())) throw new BadRequestException("Выбранное время больше недоступно по расписанию");
        return slot;
    }

    @Transactional
    public TimeSlot slotForDoctorAt(Long doctorId, LocalDateTime startsAt) {
        DoctorProfile doctor = doctors.findById(doctorId)
                .filter(DoctorProfile::isAvailableForBooking)
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        var schedule = schedules.scheduleForDoctor(doctor);
        if (!schedules.isBookable(schedule, startsAt) && doctor.isDemo()) {
            throw new BadRequestException("Выбранное время находится вне рабочего расписания или в перерыве");
        }
        TimeSlot existing = slots.findByDoctorIdAndStartsAt(doctorId, startsAt).orElse(null);
        if (existing != null) {
            if (!existing.isActive()) throw new BadRequestException("Выбранный временной слот недоступен");
            return existing;
        }
        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setStartsAt(startsAt);
        slot.setEndsAt(startsAt.plusMinutes(schedule.getSlotDurationMinutes()));
        return slots.save(slot);
    }

    private void ensureSlots(DoctorProfile doctor, com.qamqormed.entity.DoctorSchedule schedule, LocalDate start, LocalDate end) {
        List<TimeSlot> additions = new ArrayList<>();
        for (LocalDate day = start; day.isBefore(end.plusDays(1)); day = day.plusDays(1)) {
            for (var interval : schedules.workingIntervals(schedule, day)) {
                for (LocalTime time = interval.getStartTime(); time.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(interval.getEndTime()) <= 0; time = time.plusMinutes(schedule.getSlotDurationMinutes())) {
                    LocalDateTime startsAt = LocalDateTime.of(day, time);
                    if (schedules.isBookable(schedule, startsAt) && !slots.existsByDoctorIdAndStartsAt(doctor.getId(), startsAt)) {
                        TimeSlot slot = new TimeSlot(); slot.setDoctor(doctor); slot.setStartsAt(startsAt); slot.setEndsAt(startsAt.plusMinutes(schedule.getSlotDurationMinutes())); slot.setActive(true); additions.add(slot);
                    }
                }
            }
        }
        if (!additions.isEmpty()) slots.saveAll(additions);
    }
}
