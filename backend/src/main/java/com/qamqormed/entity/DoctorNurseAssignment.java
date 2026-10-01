package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "doctor_nurse_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_assignment_doctor", columnNames = "doctor_id"),
        @UniqueConstraint(name = "uk_assignment_nurse", columnNames = "nurse_id")
}, indexes = {
        @Index(name = "idx_assignments_doctor_id", columnList = "doctor_id"),
        @Index(name = "idx_assignments_nurse_id", columnList = "nurse_id")
})
public class DoctorNurseAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false, unique = true)
    private DoctorProfile doctor;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "nurse_id", nullable = false, unique = true)
    private NurseProfile nurse;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant assignedAt;
    @Column private Instant unassignedAt;

    @PrePersist void onCreate() { assignedAt = Instant.now(); }
    public Long getId() { return id; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public NurseProfile getNurse() { return nurse; }
    public void setNurse(NurseProfile nurse) { this.nurse = nurse; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(Instant unassignedAt) { this.unassignedAt = unassignedAt; }
}
