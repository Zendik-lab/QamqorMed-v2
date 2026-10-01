package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medical_certificates")
public class MedicalCertificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "medical_visit_id", nullable = false) private MedicalVisit medicalVisit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_profile_id", nullable = false) private DoctorProfile doctor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_profile_id") private PatientProfile patientProfile;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dependent_id") private Dependent dependent;
    @Column(nullable = false, length = 100) private String type;
    @Column(nullable = false) private Instant issuedAt;
    private LocalDate validUntil;
    @Column(length = 2000) private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "document_id") private MedicalDocument document;
    @PrePersist void onCreate() { issuedAt = Instant.now(); }
    public Long getId() { return id; }
    public MedicalVisit getMedicalVisit() { return medicalVisit; }
    public void setMedicalVisit(MedicalVisit medicalVisit) { this.medicalVisit = medicalVisit; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public PatientProfile getPatientProfile() { return patientProfile; }
    public void setPatientProfile(PatientProfile patientProfile) { this.patientProfile = patientProfile; }
    public Dependent getDependent() { return dependent; }
    public void setDependent(Dependent dependent) { this.dependent = dependent; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getIssuedAt() { return issuedAt; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MedicalDocument getDocument() { return document; }
    public void setDocument(MedicalDocument document) { this.document = document; }
}
