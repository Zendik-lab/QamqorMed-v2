package com.qamqormed.repository;

import com.qamqormed.entity.DoctorNurseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface DoctorNurseAssignmentRepository extends JpaRepository<DoctorNurseAssignment, Long> {
    Optional<DoctorNurseAssignment> findByDoctorId(Long doctorId);
    Optional<DoctorNurseAssignment> findByNurseId(Long nurseId);
    @Query("select a from DoctorNurseAssignment a join fetch a.nurse n join fetch n.user u join fetch n.clinic c where a.doctor.id = :doctorId and a.active = true")
    Optional<DoctorNurseAssignment> findActiveByDoctorId(Long doctorId);
    @Query("select a from DoctorNurseAssignment a join fetch a.doctor d join fetch d.user u join fetch d.clinic c where a.nurse.id = :nurseId and a.active = true")
    Optional<DoctorNurseAssignment> findActiveByNurseId(Long nurseId);
    @Query("select a from DoctorNurseAssignment a join fetch a.doctor d join fetch d.user du join fetch d.specialty ds join fetch d.clinic dc join fetch dc.city dcity join fetch dcity.region dregion join fetch a.nurse n join fetch n.user nu join fetch n.clinic nc join fetch nc.city ncity join fetch ncity.region nregion where a.doctor.id in :doctorIds and a.active = true")
    List<DoctorNurseAssignment> findActiveByDoctorIdIn(Collection<Long> doctorIds);
    @Query("select a from DoctorNurseAssignment a join fetch a.doctor d join fetch d.user du join fetch d.specialty ds join fetch d.clinic dc join fetch dc.city dcity join fetch dcity.region dregion join fetch a.nurse n join fetch n.user nu join fetch n.clinic nc join fetch nc.city ncity join fetch ncity.region nregion where a.nurse.id in :nurseIds and a.active = true")
    List<DoctorNurseAssignment> findActiveByNurseIdIn(Collection<Long> nurseIds);
    long countByDoctorId(Long doctorId);
    long countByNurseId(Long nurseId);
    List<DoctorNurseAssignment> findAllByOrderByAssignedAtDesc();
}
