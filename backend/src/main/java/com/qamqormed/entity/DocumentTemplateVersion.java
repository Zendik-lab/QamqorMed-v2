package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "document_template_versions", uniqueConstraints = @UniqueConstraint(
        name = "uk_document_template_versions_number", columnNames = {"template_id", "version_number"}), indexes = {
        @Index(name = "idx_document_template_versions_template", columnList = "template_id")
})
public class DocumentTemplateVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Lob
    @Column(nullable = false)
    private String content;

    /** Comma-separated allow-list of variables required for approval. */
    @Column(name = "required_fields", length = 2000)
    private String requiredFields;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "change_description", length = 500)
    private String changeDescription;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public DocumentTemplate getTemplate() { return template; }
    public void setTemplate(DocumentTemplate template) { this.template = template; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRequiredFields() { return requiredFields; }
    public void setRequiredFields(String requiredFields) { this.requiredFields = requiredFields; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
    public Instant getCreatedAt() { return createdAt; }
}
