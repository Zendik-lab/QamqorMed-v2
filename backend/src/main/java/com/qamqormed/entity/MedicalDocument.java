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
@Table(name = "medical_documents")
public class MedicalDocument {
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
    private MedicalDocumentType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String filePath;

    @Column(length = 1000)
    private String fileUrl;

    @Column(length = 300)
    private String fileName;

    @Column(length = 120)
    private String mimeType;

    private Long fileSize;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
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
    public MedicalDocumentType getType() { return type; }
    public void setType(MedicalDocumentType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
