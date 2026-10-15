package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ai_waitlist_proposals", indexes = {
        @Index(name = "idx_ai_waitlist_proposals_owner_status", columnList = "owner_user_id, status"),
        @Index(name = "idx_ai_waitlist_proposals_expires", columnList = "expires_at")
})
public class AiWaitlistProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id")
    private Dependent dependent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_profile_id")
    private DoctorProfile doctor;

    @Column(name = "desired_date_from", nullable = false)
    private LocalDate desiredDateFrom;

    @Column(name = "desired_date_to", nullable = false)
    private LocalDate desiredDateTo;

    @Column(name = "preferred_time_from")
    private LocalTime preferredTimeFrom;

    @Column(name = "preferred_time_to")
    private LocalTime preferredTimeTo;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiProposalStatus status = AiProposalStatus.PREPARED;

    @Column(name = "confirmed_waitlist_id")
    private Long confirmedWaitlistId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Dependent getDependent() { return dependent; }
    public void setDependent(Dependent dependent) { this.dependent = dependent; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public LocalDate getDesiredDateFrom() { return desiredDateFrom; }
    public void setDesiredDateFrom(LocalDate desiredDateFrom) { this.desiredDateFrom = desiredDateFrom; }
    public LocalDate getDesiredDateTo() { return desiredDateTo; }
    public void setDesiredDateTo(LocalDate desiredDateTo) { this.desiredDateTo = desiredDateTo; }
    public LocalTime getPreferredTimeFrom() { return preferredTimeFrom; }
    public void setPreferredTimeFrom(LocalTime preferredTimeFrom) { this.preferredTimeFrom = preferredTimeFrom; }
    public LocalTime getPreferredTimeTo() { return preferredTimeTo; }
    public void setPreferredTimeTo(LocalTime preferredTimeTo) { this.preferredTimeTo = preferredTimeTo; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public AiProposalStatus getStatus() { return status; }
    public void setStatus(AiProposalStatus status) { this.status = status; }
    public Long getConfirmedWaitlistId() { return confirmedWaitlistId; }
    public void setConfirmedWaitlistId(Long confirmedWaitlistId) { this.confirmedWaitlistId = confirmedWaitlistId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
