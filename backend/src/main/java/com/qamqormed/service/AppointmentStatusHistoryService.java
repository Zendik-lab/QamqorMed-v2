package com.qamqormed.service;

import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentStatusHistoryService {
    private final AppointmentStatusHistoryRepository history;
    public AppointmentStatusHistoryService(AppointmentStatusHistoryRepository history) { this.history = history; }
    @Transactional
    public void record(Appointment appointment, AppointmentStatus oldStatus, AppointmentStatus newStatus, User actor, String reason) {
        if (oldStatus == newStatus) return;
        AppointmentStatusHistory item = new AppointmentStatusHistory();
        item.setAppointment(appointment); item.setOldStatus(oldStatus); item.setNewStatus(newStatus); item.setChangedBy(actor); item.setReason(reason);
        history.save(item);
    }
}
