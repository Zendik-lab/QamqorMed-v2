package com.qamqormed.service;

import com.qamqormed.dto.DataIntegrityAuditResponse;
import com.qamqormed.entity.DoctorProfile;
import com.qamqormed.entity.NurseProfile;
import com.qamqormed.repository.CityRepository;
import com.qamqormed.repository.ClinicRepository;
import com.qamqormed.repository.DoctorNurseAssignmentRepository;
import com.qamqormed.repository.DoctorProfileRepository;
import com.qamqormed.repository.NurseProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataIntegrityAuditService {
    private final DoctorProfileRepository doctors;
    private final NurseProfileRepository nurses;
    private final DoctorNurseAssignmentRepository assignments;
    private final ClinicRepository clinics;
    private final CityRepository cities;

    public DataIntegrityAuditService(DoctorProfileRepository doctors, NurseProfileRepository nurses,
                                     DoctorNurseAssignmentRepository assignments, ClinicRepository clinics, CityRepository cities) {
        this.doctors = doctors; this.nurses = nurses; this.assignments = assignments; this.clinics = clinics; this.cities = cities;
    }

    @Transactional(readOnly = true)
    public DataIntegrityAuditResponse audit() {
        long doctorsWithoutClinic = 0, doctorsWithoutSpecialty = 0, doctorsWithoutCity = 0, doctorsWithoutRegion = 0;
        for (DoctorProfile doctor : doctors.findAll()) {
            if (doctor.getClinic() == null) { doctorsWithoutClinic++; continue; }
            if (doctor.getSpecialty() == null) doctorsWithoutSpecialty++;
            if (doctor.getClinic().getCity() == null) { doctorsWithoutCity++; continue; }
            if (doctor.getClinic().getCity().getRegion() == null) doctorsWithoutRegion++;
        }
        long nursesWithoutClinic = 0;
        for (NurseProfile nurse : nurses.findAll()) if (nurse.getClinic() == null) nursesWithoutClinic++;
        Set<Long> assignedNurseIds = assignments.findAll().stream()
                .filter(assignment -> assignment.getNurse() != null)
                .map(assignment -> assignment.getNurse().getId())
                .collect(Collectors.toSet());
        long nursesWithoutDoctor = nurses.findAll().stream().filter(nurse -> !assignedNurseIds.contains(nurse.getId())).count();
        long mismatch = assignments.findAll().stream()
                .filter(assignment -> assignment.getDoctor() != null && assignment.getNurse() != null
                        && assignment.getDoctor().getClinic() != null && assignment.getNurse().getClinic() != null
                        && !assignment.getDoctor().getClinic().getId().equals(assignment.getNurse().getClinic().getId()))
                .count();
        long clinicsWithoutCity = clinics.findAll().stream().filter(clinic -> clinic.getCity() == null).count();
        long citiesWithoutRegion = cities.findAll().stream().filter(city -> city.getRegion() == null).count();
        return new DataIntegrityAuditResponse(doctorsWithoutClinic, doctorsWithoutSpecialty, doctorsWithoutCity, doctorsWithoutRegion,
                nursesWithoutClinic, nursesWithoutDoctor, clinicsWithoutCity, citiesWithoutRegion, mismatch);
    }
}
