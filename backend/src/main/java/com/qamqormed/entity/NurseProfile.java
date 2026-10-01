package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nurse_profiles", indexes = {
        @Index(name = "idx_nurse_profiles_clinic_id", columnList = "clinic_id"),
        @Index(name = "idx_nurse_profiles_demo", columnList = "demo"),
        @Index(name = "idx_nurse_profiles_seed_key", columnList = "seed_key")
})
public class NurseProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id", nullable = false) private Clinic clinic;
    @Column(name = "avatar_url", length = 500) private String avatarUrl;
    @Column(name = "cabinet_number", length = 30) private String cabinetNumber;
    @Column(length = 2000) private String education;
    @Column(length = 300) private String qualification;
    @Column(nullable = false, columnDefinition = "integer default 0") private int experienceYears;
    @Column(length = 4000) private String about;
    @Column(name = "seed_key", length = 180, unique = true) private String seedKey;
    @Column(nullable = false) private boolean demo = false;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAvatar() { return avatarUrl; }
    public void setAvatar(String avatar) { this.avatarUrl = avatar; }
    public String getCabinetNumber() { return cabinetNumber; }
    public void setCabinetNumber(String cabinetNumber) { this.cabinetNumber = cabinetNumber; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSeedKey() { return seedKey; }
    public void setSeedKey(String seedKey) { this.seedKey = seedKey; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
