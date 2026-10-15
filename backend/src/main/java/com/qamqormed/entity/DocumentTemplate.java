package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_templates", indexes = {
        @Index(name = "idx_document_templates_scope", columnList = "scope"),
        @Index(name = "idx_document_templates_type", columnList = "document_type"),
        @Index(name = "idx_document_templates_language", columnList = "language"),
        @Index(name = "idx_document_templates_specialty", columnList = "specialty_id"),
        @Index(name = "idx_document_templates_clinic", columnList = "clinic_id")
})
public class DocumentTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private MedicalDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_doctor_id")
    private DoctorProfile ownerDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private TemplateLanguage language = TemplateLanguage.RU;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DocumentTemplateVersion currentVersion;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MedicalDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(MedicalDocumentType documentType) { this.documentType = documentType; }
    public TemplateScope getScope() { return scope; }
    public void setScope(TemplateScope scope) { this.scope = scope; }
    public DoctorProfile getOwnerDoctor() { return ownerDoctor; }
    public void setOwnerDoctor(DoctorProfile ownerDoctor) { this.ownerDoctor = ownerDoctor; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public TemplateLanguage getLanguage() { return language; }
    public void setLanguage(TemplateLanguage language) { this.language = language; }
    public DocumentTemplateVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(DocumentTemplateVersion currentVersion) { this.currentVersion = currentVersion; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
