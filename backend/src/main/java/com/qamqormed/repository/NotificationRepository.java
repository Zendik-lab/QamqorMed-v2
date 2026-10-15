package com.qamqormed.repository;

import com.qamqormed.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndReadFalse(Long userId);
    boolean existsByEventKey(String eventKey);
    List<Notification> findAllByRelatedAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
