package com.qamqormed.repository;

import com.qamqormed.entity.Dependent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DependentRepository extends JpaRepository<Dependent, Long> {
    List<Dependent> findAllByOwnerIdAndActiveTrueOrderByLastNameAscFirstNameAsc(Long ownerId);
    Optional<Dependent> findByIdAndOwnerIdAndActiveTrue(Long id, Long ownerId);
    Optional<Dependent> findByOwnerIdAndIinAndActiveTrue(Long ownerId, String iin);
    boolean existsByOwnerIdAndIinAndActiveTrue(Long ownerId, String iin);
}
