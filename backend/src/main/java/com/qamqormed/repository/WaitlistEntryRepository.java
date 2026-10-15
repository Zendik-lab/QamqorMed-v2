package com.qamqormed.repository;

import com.qamqormed.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.*;
import java.util.*;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findAllByPatientUserIdOrderByCreatedAtDesc(Long patientUserId);
    Optional<WaitlistEntry> findByIdAndPatientUserId(Long id, Long patientUserId);
    List<WaitlistEntry> findAllByStatusAndClinicIdAndSpecialtyIdAndDesiredDateFromLessThanEqualAndDesiredDateToGreaterThanEqual(
            WaitlistStatus status, Long clinicId, Long specialtyId, LocalDate dateTo, LocalDate dateFrom);
}
