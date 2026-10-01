package com.qamqormed.service;

import com.qamqormed.dto.TimeSlotResponse;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.TimeSlot;
import com.qamqormed.exception.NotFoundException;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeSlotService {
    private static final List<LocalTime> DEFAULT_STARTS = List.of(
            LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30),
            LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(15, 0), LocalTime.of(15, 30));

    private final TimeSlotRepository slots;
    private final DoctorProfileRepository doctors;

    public TimeSlotService(TimeSlotRepository slots, DoctorProfileRepository doctors) {
        this.slots = slots; this.doctors = doctors;
    }

    @Transactional
    public List<TimeSlotResponse> available(Long doctorId, LocalDate from, LocalDate to) {
        DoctorProfile doctor = doctors.findById(doctorId)
                .filter(d -> d.isActive() && d.getUser().isEnabled() && d.getClinic().isActive())
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(14) : to;
        if (end.isAfter(start.plusDays(31))) end = start.plusDays(31);
        if (doctor.isDemo()) ensureDemoSlots(doctor, start, end);
        return slots.findAvailable(doctorId, start.atStartOfDay(), end.plusDays(1).atStartOfDay()).stream().map(TimeSlotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TimeSlot slotForDoctor(Long slotId, Long doctorId) {
        return slots.findById(slotId)
                .filter(slot -> slot.isActive() && slot.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new NotFoundException("Время приёма не найдено"));
    }

    private void ensureDemoSlots(DoctorProfile doctor, LocalDate start, LocalDate end) {
        List<TimeSlot> additions = new ArrayList<>();
        for (LocalDate day = start; day.isBefore(end.plusDays(1)); day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (LocalTime time : DEFAULT_STARTS) {
                LocalDateTime startsAt = LocalDateTime.of(day, time);
                if (!slots.existsByDoctorIdAndStartsAt(doctor.getId(), startsAt)) {
                    TimeSlot slot = new TimeSlot(); slot.setDoctor(doctor); slot.setStartsAt(startsAt); slot.setEndsAt(startsAt.plusMinutes(30)); slot.setActive(true); additions.add(slot);
                }
            }
        }
        if (!additions.isEmpty()) slots.saveAll(additions);
    }
}
