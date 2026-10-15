package com.qamqormed.repository;

import com.qamqormed.entity.AiWaitlistProposal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiWaitlistProposalRepository extends JpaRepository<AiWaitlistProposal, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AiWaitlistProposal p where p.id = :id and p.owner.id = :ownerId")
    Optional<AiWaitlistProposal> findByIdAndOwnerIdForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
