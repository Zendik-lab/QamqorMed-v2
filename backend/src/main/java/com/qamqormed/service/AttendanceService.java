package com.qamqormed.service;

import com.qamqormed.dto.AttendanceResponse;
import com.qamqormed.entity.*;
import com.qamqormed.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {
    private final AppointmentRepository appointments; private final PatientService patients;
    public AttendanceService(AppointmentRepository appointments, PatientService patients) { this.appointments = appointments; this.patients = patients; }
    @Transactional(readOnly = true)
    public AttendanceResponse forSubject(Long ownerId, Long subjectId) {
        patients.patient(ownerId);
        final Long dependentId;
        if (patients.isOwnedDependentId(ownerId, subjectId)) dependentId = subjectId;
        else { if (!(ownerId.equals(subjectId) || patients.isOwnProfileId(ownerId, subjectId))) patients.dependent(ownerId, subjectId); dependentId = null; }
        var items = appointments.findAllByBookedByIdOrderByAppointmentAtDesc(ownerId).stream().filter(item -> dependentId == null ? item.getDependent() == null : item.getDependent() != null && dependentId.equals(item.getDependent().getId())).toList();
        long completed = items.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.COMPLETED).count();
        long cancelled = items.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.CANCELLED).count();
        long noShow = items.stream().filter(item -> item.getCanonicalStatus() == AppointmentStatus.NO_SHOW).count();
        long denominator = completed + noShow; int rate = denominator == 0 ? 0 : (int) Math.round((completed * 100.0) / denominator);
        return new AttendanceResponse(items.size(), completed, cancelled, noShow, rate);
    }
}
