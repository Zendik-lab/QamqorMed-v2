package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedule_intervals", indexes = @Index(name = "idx_schedule_intervals_schedule_day", columnList = "schedule_id, day_of_week"))
public class DoctorScheduleInterval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_id", nullable = false) private DoctorSchedule schedule;
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 12) private DayOfWeek dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "is_break", nullable = false) private boolean breakInterval;
    public Long getId() { return id; }
    public DoctorSchedule getSchedule() { return schedule; }
    public void setSchedule(DoctorSchedule value) { schedule = value; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek value) { dayOfWeek = value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime = value; }
    public boolean isBreakInterval() { return breakInterval; }
    public void setBreakInterval(boolean value) { breakInterval = value; }
}
