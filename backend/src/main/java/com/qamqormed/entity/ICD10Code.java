package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "icd10_codes", uniqueConstraints = @UniqueConstraint(name = "uk_icd10_code", columnNames = "code"), indexes = {
        @Index(name = "idx_icd10_name", columnList = "name"),
        @Index(name = "idx_icd10_active", columnList = "active")
})
public class ICD10Code {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 500) private String name;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
