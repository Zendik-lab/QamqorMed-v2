package com.qamqormed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_visits", indexes = {
        @Index(name = "idx_medical_visits_appointment", columnList = "appointment_id"),
        @Index(name = "idx_medical_visits_patient", columnList = "patient_user_id"),
        @Index(name = "idx_medical_visits_dependent", columnList = "dependent_id"),
        @Index(name = "idx_medical_visits_doctor", columnList = "doctor_user_id")
})
public class MedicalVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id")
    private Dependent dependent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private User doctorUser;

    @Column(length = 2000)
    private String chiefComplaint;

    @Column(length = 4000)
    private String anamnesis;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 30)
    private String diagnosisCode;

    @Column(length = 2000)
    private String diagnosisName;

    @Column(length = 4000)
    private String doctorNotes;

    @Column(length = 4000)
    private String recommendations;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public User getPatientUser() { return patientUser; }
    public void setPatientUser(User patientUser) { this.patientUser = patientUser; }
    public Dependent getDependent() { return dependent; }
    public void setDependent(Dependent dependent) { this.dependent = dependent; }
    public User getDoctorUser() { return doctorUser; }
    public void setDoctorUser(User doctorUser) { this.doctorUser = doctorUser; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public String getAnamnesis() { return anamnesis; }
    public void setAnamnesis(String anamnesis) { this.anamnesis = anamnesis; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getDiagnosisCode() { return diagnosisCode; }
    public void setDiagnosisCode(String diagnosisCode) { this.diagnosisCode = diagnosisCode; }
    public String getDiagnosisName() { return diagnosisName; }
    public void setDiagnosisName(String diagnosisName) { this.diagnosisName = diagnosisName; }
    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
