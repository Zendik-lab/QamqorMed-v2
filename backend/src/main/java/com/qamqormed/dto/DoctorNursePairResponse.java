package com.qamqormed.dto;

import com.qamqormed.entity.DoctorNurseAssignment;

public record DoctorNursePairResponse(Long id, Long doctorId, String doctorName, Long nurseId, String nurseName, Long clinicId, String clinicName, boolean active) {
    public static DoctorNursePairResponse from(DoctorNurseAssignment value) {
        var doctor = value.getDoctor(); var nurse = value.getNurse();
        return new DoctorNursePairResponse(value.getId(), doctor.getId(), fullName(doctor.getUser()), nurse.getId(), fullName(nurse.getUser()), doctor.getClinic().getId(), doctor.getClinic().getName(), value.isActive());
    }
    private static String fullName(com.qamqormed.entity.User user) { return String.join(" ", user.getLastName(), user.getFirstName(), user.getMiddleName() == null ? "" : user.getMiddleName()).trim().replaceAll(" +", " "); }
}
