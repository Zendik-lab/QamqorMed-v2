package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "doctor_profiles", indexes = {
        @Index(name = "idx_doctor_profiles_clinic_id", columnList = "clinic_id"),
        @Index(name = "idx_doctor_profiles_specialty_id", columnList = "specialty_id"),
        @Index(name = "idx_doctor_profiles_demo", columnList = "demo"),
        @Index(name = "idx_doctor_profiles_seed_key", columnList = "seed_key")
})
public class DoctorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @OneToOne(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DoctorSchedule schedule;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "specialty_id", nullable = false) private Specialty specialty;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id", nullable = false) private Clinic clinic;
    @Column(nullable = false) private int experienceYears;
    @Column(length = 2000) private String bio;
    @Column(name = "avatar_url", length = 500) private String avatarUrl;
    @Column(name = "qualification_category", length = 120) private String qualificationCategory;
    @Column(length = 2000) private String education;
    @Column(length = 30) private String officeNumber;
    @Column(name = "consultation_duration_minutes", nullable = false, columnDefinition = "integer default 30") private int consultationDurationMinutes = 30;
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
    public DoctorSchedule getSchedule() { return schedule; }
    public void setSchedule(DoctorSchedule schedule) { this.schedule = schedule; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAvatar() { return avatarUrl; }
    public void setAvatar(String avatar) { this.avatarUrl = avatar; }
    public String getQualificationCategory() { return qualificationCategory; }
    public void setQualificationCategory(String qualificationCategory) { this.qualificationCategory = qualificationCategory; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getOfficeNumber() { return officeNumber; }
    public void setOfficeNumber(String officeNumber) { this.officeNumber = officeNumber; }
    public String getCabinetNumber() { return officeNumber; }
    public void setCabinetNumber(String cabinetNumber) { this.officeNumber = cabinetNumber; }
    public int getConsultationDurationMinutes() { return consultationDurationMinutes; }
    public void setConsultationDurationMinutes(int consultationDurationMinutes) { this.consultationDurationMinutes = consultationDurationMinutes; }
    public int getConsultationDuration() { return consultationDurationMinutes; }
    public void setConsultationDuration(int consultationDuration) { this.consultationDurationMinutes = consultationDuration; }
    public String getAbout() { return about == null || about.isBlank() ? bio : about; }
    public void setAbout(String about) { this.about = about; this.bio = about; }
    public String getSeedKey() { return seedKey; }
    public void setSeedKey(String seedKey) { this.seedKey = seedKey; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    /** Catalog and booking availability is independent of the account's ability to log in. */
    public boolean isAvailableForBooking() {
        return active && specialty.isActive() && clinic.isActive()
                && clinic.getCity().isActive() && clinic.getCity().getRegion().isActive();
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
