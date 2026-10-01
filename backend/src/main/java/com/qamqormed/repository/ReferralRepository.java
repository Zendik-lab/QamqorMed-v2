package com.qamqormed.repository;

import com.qamqormed.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findAllByMedicalVisitIdOrderByCreatedAtAsc(Long medicalVisitId);
}
