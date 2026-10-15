package com.qamqormed.repository;

import com.qamqormed.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
    List<Referral> findAllByDoctorUserIdOrderByIssuedAtDesc(Long doctorUserId);
    Optional<Referral> findByIdAndDoctorUserId(Long id, Long doctorUserId);
}
