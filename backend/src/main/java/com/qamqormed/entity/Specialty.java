package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "specialties", uniqueConstraints = {
        @UniqueConstraint(name = "uk_specialties_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_specialties_code", columnNames = "code")
})
public class Specialty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 150) private String name;
    @Column(length = 60) private String code;
    @Column(name = "name_ru", length = 150) private String nameRu;
    @Column(name = "name_kk", length = 150) private String nameKk;
    @Column(name = "name_en", length = 150) private String nameEn;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { normalizeNames(); Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { normalizeNames(); updatedAt = Instant.now(); }
    private void normalizeNames() {
        if (nameRu == null || nameRu.isBlank()) nameRu = name;
        if (name == null || name.isBlank()) name = nameRu;
        if (nameKk == null || nameKk.isBlank()) nameKk = nameRu;
        if (nameEn == null || nameEn.isBlank()) nameEn = nameRu;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameRu() { return nameRu == null ? name : nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; this.name = nameRu; }
    public String getNameKk() { return nameKk == null ? getNameRu() : nameKk; }
    public void setNameKk(String nameKk) { this.nameKk = nameKk; }
    public String getNameEn() { return nameEn == null ? getNameRu() : nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
