package com.qamqormed.repository;

import com.qamqormed.entity.DoctorFavoriteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorFavoriteTemplateRepository extends JpaRepository<DoctorFavoriteTemplate, Long> {
    List<DoctorFavoriteTemplate> findAllByDoctorId(Long doctorId);
    Optional<DoctorFavoriteTemplate> findByDoctorIdAndTemplateId(Long doctorId, Long templateId);
    boolean existsByDoctorIdAndTemplateId(Long doctorId, Long templateId);
}
