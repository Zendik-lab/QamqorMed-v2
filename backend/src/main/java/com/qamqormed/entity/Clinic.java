package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "clinics", uniqueConstraints = @UniqueConstraint(name = "uk_clinics_external_source_id", columnNames = "external_source_id"), indexes = {
        @Index(name = "idx_clinics_city_id", columnList = "city_id"),
        @Index(name = "idx_clinics_region_id", columnList = "region_id"),
        @Index(name = "idx_clinics_external_source_id", columnList = "external_source_id")
})
public class Clinic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "city_id", nullable = false) private City city;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "region_id") private Region region;
    @Column(length = 300) private String address;
    @Column(length = 30) private String phone;
    @Column(length = 2000) private String description;
    @Column(length = 40) private String type;
    @Column(length = 40) private String source;
    @Column(name = "external_source_id", length = 100) private String externalSourceId;
    @Column(name = "is_real", nullable = false) private boolean real;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "clinic", fetch = FetchType.LAZY)
    private java.util.List<PatientProfile> patientProfiles = new java.util.ArrayList<>();
    @PrePersist void onCreate() { normalizeSource(); Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { normalizeSource(); updatedAt = Instant.now(); }
    @PreRemove void detachPatientProfiles() { patientProfiles.forEach(profile -> profile.setClinic(null)); }
    private void normalizeSource() {
        if (region == null && city != null) region = city.getRegion();
        if (type == null || type.isBlank()) type = "MEDICAL_ORGANIZATION";
        if (source == null || source.isBlank()) source = real ? "EGOV_OPEN_DATA" : "DEMO";
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    public Region getRegion() { return region == null && city != null ? city.getRegion() : region; }
    public void setRegion(Region region) { this.region = region; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalSourceId() { return externalSourceId; }
    public void setExternalSourceId(String externalSourceId) { this.externalSourceId = externalSourceId; }
    public boolean isReal() { return real; }
    public void setReal(boolean real) { this.real = real; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
