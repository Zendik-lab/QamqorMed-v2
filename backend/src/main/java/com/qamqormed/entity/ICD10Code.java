package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "icd10_codes", uniqueConstraints = @UniqueConstraint(name = "uk_icd10_code", columnNames = "code"), indexes = {
        @Index(name = "idx_icd10_name", columnList = "name"),
        @Index(name = "idx_icd10_title_ru", columnList = "title_ru"),
        @Index(name = "idx_icd10_title_kk", columnList = "title_kk"),
        @Index(name = "idx_icd10_title_en", columnList = "title_en"),
        @Index(name = "idx_icd10_active", columnList = "active")
})
public class ICD10Code {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 500) private String name;
    /** Legacy display name retained for existing clients and rows. */
    @Column(name = "title_ru", length = 500) private String titleRu;
    @Column(name = "title_kk", length = 500) private String titleKk;
    @Column(name = "title_en", length = 500) private String titleEn;
    @Column(length = 2000) private String description;
    @Column(length = 20) private String chapter;
    @Column(name = "parent_code", length = 20) private String parentCode;
    // Dev databases may already contain ICD-10 rows before this column is added.
    // The dev SQL initializer supplies the default; production Flyway makes it NOT NULL.
    @Column private boolean selectable = true;
    @Column(length = 120) private String source;
    @Column(name = "source_version", length = 80) private String sourceVersion;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { normalizeTitles(); Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { normalizeTitles(); updatedAt = Instant.now(); }
    private void normalizeTitles() {
        if (titleRu == null || titleRu.isBlank()) titleRu = name;
        if (name == null || name.isBlank()) name = titleRu;
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitleRu() { return titleRu == null ? name : titleRu; }
    public void setTitleRu(String titleRu) { this.titleRu = titleRu; this.name = titleRu; }
    public String getTitleKk() { return titleKk; }
    public void setTitleKk(String titleKk) { this.titleKk = titleKk; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public boolean isSelectable() { return selectable; }
    public void setSelectable(boolean selectable) { this.selectable = selectable; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
