package com.qamqormed.service;

import com.qamqormed.dto.*;
import com.qamqormed.entity.*;
import com.qamqormed.exception.BadRequestException;
import com.qamqormed.exception.ConflictException;
import com.qamqormed.repository.AppointmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Qyzylorda");
    private final DoctorScheduleRepository schedules;
    private final DoctorProfileRepository doctors;
    private final AppointmentRepository appointments;

    public ScheduleService(DoctorScheduleRepository schedules, DoctorProfileRepository doctors, AppointmentRepository appointments) {
        this.schedules = schedules; this.doctors = doctors; this.appointments = appointments;
    }

    @Transactional
    public DoctorSchedule scheduleForDoctor(Long doctorId) {
        DoctorProfile doctor = doctors.findById(doctorId).orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Врач не найден"));
        return scheduleForDoctor(doctor);
    }

    @Transactional
    public DoctorSchedule scheduleForDoctor(DoctorProfile doctor) {
        return schedules.findByDoctorId(doctor.getId()).map(value -> {
            doctor.setSchedule(value);
            return value;
        }).orElseGet(() -> {
            DoctorSchedule value = defaultSchedule(doctor);
            doctor.setSchedule(value);
            return schedules.save(value);
        });
    }

    @Transactional
    public DoctorScheduleResponse response(Long doctorId) { return DoctorScheduleResponse.from(scheduleForDoctor(doctorId)); }

    @Transactional
    public DoctorScheduleResponse update(Long doctorId, DoctorScheduleUpdateRequest request) {
        DoctorProfile doctor = doctors.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new com.qamqormed.exception.NotFoundException("Врач не найден"));
        DoctorSchedule schedule = scheduleForDoctor(doctor);
        int slotDuration = request.slotDurationMinutes() == null ? schedule.getSlotDurationMinutes() : request.slotDurationMinutes();
        List<ScheduleIntervalRequest> intervals = request.intervals() == null ? List.of() : request.intervals();
        List<ScheduleExceptionRequest> exceptions = request.exceptions() == null ? List.of() : request.exceptions();
        validate(slotDuration, intervals, exceptions);
        ensureNoAppointmentConflict(doctorId, exceptions, slotDuration);
        schedule.setSlotDurationMinutes(slotDuration);
        schedule.getIntervals().clear();
        intervals.forEach(item -> {
            DoctorScheduleInterval value = new DoctorScheduleInterval();
            value.setSchedule(schedule); value.setDayOfWeek(item.dayOfWeek()); value.setStartTime(item.startTime()); value.setEndTime(item.endTime()); value.setBreakInterval(item.breakInterval());
            schedule.getIntervals().add(value);
        });
        schedule.getExceptions().clear();
        exceptions.forEach(item -> {
            ScheduleException value = new ScheduleException();
            value.setSchedule(schedule); value.setDate(item.date()); value.setType(item.type()); value.setStartTime(item.startTime()); value.setEndTime(item.endTime()); value.setNote(trim(item.note()));
            schedule.getExceptions().add(value);
        });
        return DoctorScheduleResponse.from(schedules.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleInterval> workingIntervals(DoctorSchedule schedule, LocalDate date) {
        Optional<ScheduleException> exception = schedule.getExceptions().stream().filter(item -> date.equals(item.getDate())).findFirst();
        if (exception.isPresent()) {
            ScheduleException value = exception.get();
            if (value.getType() != ScheduleExceptionType.CUSTOM_WORKING_HOURS || value.getStartTime() == null || value.getEndTime() == null) return List.of();
            DoctorScheduleInterval interval = new DoctorScheduleInterval(); interval.setDayOfWeek(date.getDayOfWeek()); interval.setStartTime(value.getStartTime()); interval.setEndTime(value.getEndTime()); interval.setBreakInterval(false);
            return List.of(interval);
        }
        return schedule.getIntervals().stream().filter(item -> item.getDayOfWeek() == date.getDayOfWeek() && !item.isBreakInterval()).toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleInterval> breaks(DoctorSchedule schedule, LocalDate date) {
        if (schedule.getExceptions().stream().anyMatch(item -> date.equals(item.getDate()))) return List.of();
        return schedule.getIntervals().stream().filter(item -> item.getDayOfWeek() == date.getDayOfWeek() && item.isBreakInterval()).toList();
    }

    @Transactional(readOnly = true)
    public boolean isBookable(DoctorSchedule schedule, LocalDateTime startsAt) {
        if (!schedule.isActive()) return false;
        LocalDate date = startsAt.toLocalDate();
        LocalTime end = startsAt.toLocalTime().plusMinutes(schedule.getSlotDurationMinutes());
        if (!end.isAfter(startsAt.toLocalTime())) return false;
        return workingIntervals(schedule, date).stream().anyMatch(interval -> !startsAt.toLocalTime().isBefore(interval.getStartTime()) && !end.isAfter(interval.getEndTime()))
                && breaks(schedule, date).stream().noneMatch(item -> startsAt.toLocalTime().isBefore(item.getEndTime()) && end.isAfter(item.getStartTime()));
    }

    @Transactional(readOnly = true)
    public Optional<ScheduleException> exceptionFor(DoctorSchedule schedule, LocalDate date) {
        return schedule.getExceptions().stream().filter(item -> date.equals(item.getDate())).findFirst();
    }

    private DoctorSchedule defaultSchedule(DoctorProfile doctor) {
        DoctorSchedule schedule = new DoctorSchedule(); schedule.setDoctor(doctor);
        int pattern = doctor.isDemo() && doctor.getId() != null ? Math.floorMod(doctor.getId().intValue(), 3) : 0;
        schedule.setSlotDurationMinutes(pattern == 1 ? 20 : pattern == 2 ? 45 : 30);
        List<DayOfWeek> days = pattern == 0 ? List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                : pattern == 1 ? List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                : List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        LocalTime start = pattern == 2 ? LocalTime.of(8, 0) : pattern == 1 ? LocalTime.of(10, 0) : LocalTime.of(9, 0);
        LocalTime end = pattern == 2 ? LocalTime.of(14, 0) : pattern == 1 ? LocalTime.of(17, 0) : LocalTime.of(13, 0);
        for (DayOfWeek day : days) addInterval(schedule, day, start, end, false);
        if (pattern == 0) for (DayOfWeek day : days) {
            addInterval(schedule, day, LocalTime.of(13, 0), LocalTime.of(14, 0), true);
            addInterval(schedule, day, LocalTime.of(14, 0), LocalTime.of(18, 0), false);
        }
        return schedule;
    }

    private void addInterval(DoctorSchedule schedule, DayOfWeek day, LocalTime start, LocalTime end, boolean breakInterval) {
        DoctorScheduleInterval value = new DoctorScheduleInterval(); value.setSchedule(schedule); value.setDayOfWeek(day); value.setStartTime(start); value.setEndTime(end); value.setBreakInterval(breakInterval); schedule.getIntervals().add(value);
    }

    private void validate(int slotDuration, List<ScheduleIntervalRequest> intervals, List<ScheduleExceptionRequest> exceptions) {
        if (slotDuration < 5 || slotDuration > 240) throw new BadRequestException("Длительность слота должна быть от 5 до 240 минут");
        intervals.forEach(item -> {
            if (item == null || item.dayOfWeek() == null) throw new BadRequestException("Укажите день недели");
            if (item.startTime() == null || item.endTime() == null || !item.endTime().isAfter(item.startTime())) throw new BadRequestException("Время окончания должно быть позже времени начала");
        });
        Map<DayOfWeek, List<ScheduleIntervalRequest>> byDay = intervals.stream().collect(Collectors.groupingBy(ScheduleIntervalRequest::dayOfWeek));
        byDay.forEach((day, values) -> {
            if (day == null) throw new BadRequestException("Укажите день недели");
            List<ScheduleIntervalRequest> ordered = values.stream().sorted(Comparator.comparing(ScheduleIntervalRequest::startTime)).toList();
            for (int i = 0; i < ordered.size(); i++) {
                ScheduleIntervalRequest current = ordered.get(i);
                if (current.startTime() == null || current.endTime() == null || !current.endTime().isAfter(current.startTime())) throw new BadRequestException("Время окончания должно быть позже времени начала");
                if (i > 0 && current.startTime().isBefore(ordered.get(i - 1).endTime())) {
                    ScheduleIntervalRequest previous = ordered.get(i - 1);
                    boolean breakInsideWork = current.breakInterval() && !previous.breakInterval() && !current.startTime().isBefore(previous.startTime()) && !current.endTime().isAfter(previous.endTime());
                    if (!breakInsideWork) throw new BadRequestException("Интервалы и перерывы не должны пересекаться");
                }
            }
            List<ScheduleIntervalRequest> work = ordered.stream().filter(item -> !item.breakInterval()).toList();
            ordered.stream().filter(ScheduleIntervalRequest::breakInterval).forEach(breakItem -> {
                boolean insideDay = !work.isEmpty() && (work.stream().anyMatch(item -> !item.endTime().isAfter(breakItem.startTime())) && work.stream().anyMatch(item -> !item.startTime().isBefore(breakItem.endTime()))
                        || work.stream().anyMatch(item -> !item.startTime().isAfter(breakItem.startTime()) && !item.endTime().isBefore(breakItem.endTime())));
                if (!insideDay) throw new BadRequestException("Перерыв должен находиться внутри рабочего дня");
            });
        });
        exceptions.forEach(item -> {
            if (item.date() == null || item.type() == null) throw new BadRequestException("Некорректное исключение расписания");
            boolean custom = item.type() == ScheduleExceptionType.CUSTOM_WORKING_HOURS;
            if (custom && (item.startTime() == null || item.endTime() == null || !item.endTime().isAfter(item.startTime()))) throw new BadRequestException("Для особого рабочего дня укажите корректный интервал");
            if (!custom && (item.startTime() != null || item.endTime() != null)) throw new BadRequestException("Для выходного или отпуска время не указывается");
        });
    }

    private void ensureNoAppointmentConflict(Long doctorId, List<ScheduleExceptionRequest> exceptions, int slotDuration) {
        for (ScheduleExceptionRequest exception : exceptions) {
            if (exception.type() == ScheduleExceptionType.DAY_OFF || exception.type() == ScheduleExceptionType.VACATION || exception.type() == ScheduleExceptionType.UNAVAILABLE) {
                LocalDateTime from = exception.date().atStartOfDay();
                if (!appointments.findAllByDoctorIdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(doctorId, from, from.plusDays(1)).stream().noneMatch(item -> item.getCanonicalStatus() != AppointmentStatus.CANCELLED)) {
                    throw new ConflictException("Нельзя закрыть дату: на неё уже есть записи пациентов");
                }
            } else if (exception.type() == ScheduleExceptionType.CUSTOM_WORKING_HOURS) {
                LocalDateTime from = exception.date().atStartOfDay();
                boolean conflict = appointments.findAllByDoctorIdAndAppointmentAtGreaterThanEqualAndAppointmentAtLessThanOrderByAppointmentAtAsc(doctorId, from, from.plusDays(1)).stream()
                        .filter(item -> item.getCanonicalStatus() != AppointmentStatus.CANCELLED)
                        .anyMatch(item -> item.getAppointmentAt().toLocalTime().isBefore(exception.startTime())
                                || item.getAppointmentAt().toLocalTime().plusMinutes(slotDuration).isAfter(exception.endTime()));
                if (conflict) throw new ConflictException("Особый рабочий день не покрывает существующие записи пациентов");
            }
        }
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
