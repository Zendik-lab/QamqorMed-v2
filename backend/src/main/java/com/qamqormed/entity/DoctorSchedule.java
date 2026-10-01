package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctor_schedules", uniqueConstraints = @UniqueConstraint(name = "uk_doctor_schedule_doctor", columnNames = "doctor_id"))
public class DoctorSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false, unique = true) private DoctorProfile doctor;
    @Column(name = "slot_duration_minutes", nullable = false) private int slotDurationMinutes = 30;
    @Column(name = "time_zone", nullable = false, length = 60) private String timeZone = "Asia/Qyzylorda";
    @Column(nullable = false) private boolean active = true;
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoctorScheduleInterval> intervals = new ArrayList<>();
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleException> exceptions = new ArrayList<>();
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public int getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(int value) { this.slotDurationMinutes = value; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String value) { this.timeZone = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { this.active = value; }
    public List<DoctorScheduleInterval> getIntervals() { return intervals; }
    public List<ScheduleException> getExceptions() { return exceptions; }
}
