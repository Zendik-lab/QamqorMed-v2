package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "doctor_default_templates", uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_default_template_type", columnNames = {"doctor_id", "document_type"}))
public class DoctorDefaultTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private DoctorProfile doctor;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 30) private MedicalDocumentType documentType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "template_id", nullable = false) private DocumentTemplate template;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist @PreUpdate void onChange() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }
    public MedicalDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(MedicalDocumentType documentType) { this.documentType = documentType; }
    public DocumentTemplate getTemplate() { return template; }
    public void setTemplate(DocumentTemplate template) { this.template = template; }
    public Instant getUpdatedAt() { return updatedAt; }
}
