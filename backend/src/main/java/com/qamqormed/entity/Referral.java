package com.qamqormed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "referrals")
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_visit_id", nullable = false)
    private MedicalVisit medicalVisit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id")
    private Dependent dependent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private User doctorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferralType referralType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_specialty_id")
    private Specialty targetSpecialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_clinic_id")
    private Clinic targetClinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_doctor_id")
    private DoctorProfile targetDoctor;

    @Column(length = 2000)
    private String reason;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferralStatus status = ReferralStatus.CREATED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant issuedAt;
    private java.time.LocalDate validUntil;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); issuedAt = createdAt; updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public MedicalVisit getMedicalVisit() { return medicalVisit; }
    public void setMedicalVisit(MedicalVisit medicalVisit) { this.medicalVisit = medicalVisit; }
    public User getPatientUser() { return patientUser; }
    public void setPatientUser(User patientUser) { this.patientUser = patientUser; }
    public Dependent getDependent() { return dependent; }
    public void setDependent(Dependent dependent) { this.dependent = dependent; }
    public User getDoctorUser() { return doctorUser; }
    public void setDoctorUser(User doctorUser) { this.doctorUser = doctorUser; }
    public ReferralType getReferralType() { return referralType; }
    public void setReferralType(ReferralType referralType) { this.referralType = referralType; }
    public Specialty getTargetSpecialty() { return targetSpecialty; }
    public void setTargetSpecialty(Specialty targetSpecialty) { this.targetSpecialty = targetSpecialty; }
    public Clinic getTargetClinic() { return targetClinic; }
    public void setTargetClinic(Clinic targetClinic) { this.targetClinic = targetClinic; }
    public DoctorProfile getTargetDoctor() { return targetDoctor; }
    public void setTargetDoctor(DoctorProfile targetDoctor) { this.targetDoctor = targetDoctor; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public java.time.LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(java.time.LocalDate validUntil) { this.validUntil = validUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}
