package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visit_diagnoses", indexes = @Index(name = "idx_visit_diagnoses_visit", columnList = "medical_visit_id"))
public class VisitDiagnosis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "medical_visit_id", nullable = false) private MedicalVisit medicalVisit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "icd10_code_id", nullable = false) private ICD10Code icd10Code;
    // Nullable for legacy rows created before the snapshot migration. New diagnoses
    // always receive immutable snapshots in ClinicalStructuredService.
    @Column(name = "icd10_code_snapshot", length = 20) private String icd10CodeSnapshot;
    @Column(name = "icd10_name_snapshot", length = 500) private String icd10NameSnapshot;
    @Column(length = 1000) private String notes;
    @Column(nullable = false) private boolean primaryDiagnosis;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DiagnosisType diagnosisType = DiagnosisType.PRIMARY;
    @Column(nullable = false, length = 30) private String status = "ACTIVE";
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public MedicalVisit getMedicalVisit() { return medicalVisit; }
    public void setMedicalVisit(MedicalVisit medicalVisit) { this.medicalVisit = medicalVisit; }
    public ICD10Code getIcd10Code() { return icd10Code; }
    public void setIcd10Code(ICD10Code icd10Code) { this.icd10Code = icd10Code; }
    public String getIcd10CodeSnapshot() { return icd10CodeSnapshot; }
    public void setIcd10CodeSnapshot(String icd10CodeSnapshot) { this.icd10CodeSnapshot = icd10CodeSnapshot; }
    public String getIcd10NameSnapshot() { return icd10NameSnapshot; }
    public void setIcd10NameSnapshot(String icd10NameSnapshot) { this.icd10NameSnapshot = icd10NameSnapshot; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isPrimaryDiagnosis() { return primaryDiagnosis; }
    public void setPrimaryDiagnosis(boolean primaryDiagnosis) { this.primaryDiagnosis = primaryDiagnosis; }
    public DiagnosisType getDiagnosisType() { return diagnosisType; }
    public void setDiagnosisType(DiagnosisType diagnosisType) { this.diagnosisType = diagnosisType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
