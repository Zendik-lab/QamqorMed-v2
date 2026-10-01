package com.qamqormed.repository;

import com.qamqormed.entity.ClinicSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClinicSpecialtyRepository extends JpaRepository<ClinicSpecialty, Long> {
    Optional<ClinicSpecialty> findByClinicIdAndSpecialtyId(Long clinicId, Long specialtyId);
    @Query("select cs from ClinicSpecialty cs join fetch cs.specialty s where cs.clinic.id = :clinicId and s.active = true order by s.name")
    List<ClinicSpecialty> findPublicByClinicId(Long clinicId);
}
