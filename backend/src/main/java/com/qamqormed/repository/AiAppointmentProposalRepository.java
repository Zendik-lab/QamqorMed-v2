package com.qamqormed.repository;

import com.qamqormed.entity.AiAppointmentProposal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiAppointmentProposalRepository extends JpaRepository<AiAppointmentProposal, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AiAppointmentProposal p where p.id = :id and p.owner.id = :ownerId")
    Optional<AiAppointmentProposal> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
