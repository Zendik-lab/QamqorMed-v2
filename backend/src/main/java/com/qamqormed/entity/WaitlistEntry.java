package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "waitlist_entries", indexes = {
        @Index(name = "idx_waitlist_matching", columnList = "status, clinic_id, specialty_id, desired_date_from, desired_date_to"),
        @Index(name = "idx_waitlist_patient", columnList = "patient_user_id")
})
public class WaitlistEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_user_id", nullable = false) @OnDelete(action = OnDeleteAction.CASCADE) private User patientUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dependent_id") private Dependent dependent;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id", nullable = false) private Clinic clinic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "specialty_id", nullable = false) private Specialty specialty;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doctor_profile_id") private DoctorProfile doctor;
    @Column(name = "desired_date_from", nullable = false) private LocalDate desiredDateFrom;
    @Column(name = "desired_date_to", nullable = false) private LocalDate desiredDateTo;
    @Column(name = "preferred_time_from") private LocalTime preferredTimeFrom;
    @Column(name = "preferred_time_to") private LocalTime preferredTimeTo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private WaitlistStatus status = WaitlistStatus.ACTIVE;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public User getPatientUser() { return patientUser; }
    public void setPatientUser(User patientUser) { this.patientUser = patientUser; }
    public Dependent getDependent() { return dependent; }
    public void setDependent(Dependent dependent) { this.dependent = dependent; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public LocalDate getDesiredDateFrom() { return desiredDateFrom; }
    public void setDesiredDateFrom(LocalDate value) { desiredDateFrom = value; }
    public LocalDate getDesiredDateTo() { return desiredDateTo; }
    public void setDesiredDateTo(LocalDate value) { desiredDateTo = value; }
    public LocalTime getPreferredTimeFrom() { return preferredTimeFrom; }
    public void setPreferredTimeFrom(LocalTime value) { preferredTimeFrom = value; }
    public LocalTime getPreferredTimeTo() { return preferredTimeTo; }
    public void setPreferredTimeTo(LocalTime value) { preferredTimeTo = value; }
    public WaitlistStatus getStatus() { return status; }
    public void setStatus(WaitlistStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
