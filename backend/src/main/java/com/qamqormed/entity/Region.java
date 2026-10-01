package com.qamqormed.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "regions", uniqueConstraints = @UniqueConstraint(name = "uk_regions_code", columnNames = "code"))
public class Region {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(name = "name_ru", length = 150) private String nameRu;
    @Column(name = "name_kk", length = 150) private String nameKk;
    @Column(name = "name_en", length = 150) private String nameEn;
    @Column(length = 30) private String type;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private java.util.List<PatientProfile> patientProfiles = new java.util.ArrayList<>();

    @PrePersist void onCreate() { normalizeNames(); Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { normalizeNames(); updatedAt = Instant.now(); }
    @PreRemove void detachPatientProfiles() { patientProfiles.forEach(profile -> profile.setRegion(null)); }
    private void normalizeNames() {
        if (nameRu == null || nameRu.isBlank()) nameRu = name;
        if (name == null || name.isBlank()) name = nameRu;
        if (nameKk == null || nameKk.isBlank()) nameKk = nameRu;
        if (nameEn == null || nameEn.isBlank()) nameEn = nameRu;
        if (type == null || type.isBlank()) type = "REGION";
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameRu() { return nameRu == null ? name : nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; this.name = nameRu; }
    public String getNameKk() { return nameKk == null ? getNameRu() : nameKk; }
    public void setNameKk(String nameKk) { this.nameKk = nameKk; }
    public String getNameEn() { return nameEn == null ? getNameRu() : nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getType() { return type == null ? "REGION" : type; }
    public void setType(String type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
