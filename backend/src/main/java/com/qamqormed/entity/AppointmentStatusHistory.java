package com.qamqormed.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "appointment_status_history", indexes = {
        @Index(name = "idx_appointment_status_history_appointment", columnList = "appointment_id, changed_at")
})
public class AppointmentStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Appointment appointment;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private AppointmentStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private AppointmentStatus newStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User changedBy;
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
    @Column(length = 500)
    private String reason;
    @PrePersist void onCreate() { if (changedAt == null) changedAt = Instant.now(); }
    public Long getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public AppointmentStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(AppointmentStatus oldStatus) { this.oldStatus = oldStatus; }
    public AppointmentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(AppointmentStatus newStatus) { this.newStatus = newStatus; }
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
