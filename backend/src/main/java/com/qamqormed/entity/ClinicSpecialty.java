package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Links a clinic to a catalog specialty. demo=true means the source did not publish service-level data. */
@Entity
@Table(name = "clinic_specialties", uniqueConstraints = @UniqueConstraint(
        name = "uk_clinic_specialty", columnNames = {"clinic_id", "specialty_id"}), indexes = {
        @Index(name = "idx_clinic_specialties_clinic_id", columnList = "clinic_id"),
        @Index(name = "idx_clinic_specialties_specialty_id", columnList = "specialty_id")
})
public class ClinicSpecialty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;
    @Column(nullable = false) private boolean demo = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Clinic getClinic() { return clinic; }
    public void setClinic(Clinic clinic) { this.clinic = clinic; }
    public Specialty getSpecialty() { return specialty; }
    public void setSpecialty(Specialty specialty) { this.specialty = specialty; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
