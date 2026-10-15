package com.qamqormed.service;

import com.qamqormed.dto.AdminAppointmentStatisticsResponse;
import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class AdminAppointmentStatisticsService {
    private final AppointmentRepository appointments;
    public AdminAppointmentStatisticsService(AppointmentRepository appointments) { this.appointments = appointments; }
    @Transactional(readOnly = true)
    public AdminAppointmentStatisticsResponse stats(LocalDate from, LocalDate to, Long regionId, Long cityId, Long clinicId, Long doctorId) {
        var values = appointments.findAll().stream().filter(item -> from == null || !item.getAppointmentAt().toLocalDate().isBefore(from))
                .filter(item -> to == null || !item.getAppointmentAt().toLocalDate().isAfter(to))
                .filter(item -> regionId == null || item.getDoctor().getClinic().getCity().getRegion().getId().equals(regionId))
                .filter(item -> cityId == null || item.getDoctor().getClinic().getCity().getId().equals(cityId))
                .filter(item -> clinicId == null || item.getDoctor().getClinic().getId().equals(clinicId))
                .filter(item -> doctorId == null || item.getDoctor().getId().equals(doctorId)).toList();
        long completed = values.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = values.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.CANCELLED).count();
        long noShow = values.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.NO_SHOW).count();
        long confirmed = values.stream().filter(item -> item.getConfirmationStatus() == AppointmentConfirmationStatus.CONFIRMED).count();
        long pending = values.stream().filter(item -> item.getConfirmationStatus() == null || item.getConfirmationStatus() == AppointmentConfirmationStatus.PENDING).count();
        long denominator = completed + noShow;
        return new AdminAppointmentStatisticsResponse(values.size(), completed, cancelled, noShow, confirmed, pending, denominator == 0 ? 0 : (int) Math.round(noShow * 100.0 / denominator));
    }
}
