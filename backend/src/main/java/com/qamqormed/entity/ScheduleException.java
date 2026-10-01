package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_exceptions", uniqueConstraints = @UniqueConstraint(name = "uk_schedule_exception_date", columnNames = {"schedule_id", "exception_date"}))
public class ScheduleException {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "schedule_id", nullable = false) private DoctorSchedule schedule;
    @Column(name = "exception_date", nullable = false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private ScheduleExceptionType type;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(length = 300) private String note;
    public Long getId() { return id; }
    public DoctorSchedule getSchedule() { return schedule; }
    public void setSchedule(DoctorSchedule value) { schedule = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public ScheduleExceptionType getType() { return type; }
    public void setType(ScheduleExceptionType value) { type = value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
}
