package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medication_prescriptions", indexes = @Index(name = "idx_medication_prescriptions_visit", columnList = "medical_visit_id"))
public class MedicationPrescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "medical_visit_id", nullable = false) private MedicalVisit medicalVisit;
    @Column(nullable = false, length = 300) private String medicationName;
    @Column(length = 200) private String dosage;
    @Column(length = 200) private String frequency;
    @Column(length = 100) private String duration;
    @Column(length = 1000) private String instructions;
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public MedicalVisit getMedicalVisit() { return medicalVisit; }
    public void setMedicalVisit(MedicalVisit medicalVisit) { this.medicalVisit = medicalVisit; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Instant getCreatedAt() { return createdAt; }
}
