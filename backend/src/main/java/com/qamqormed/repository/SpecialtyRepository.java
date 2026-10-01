package com.qamqormed.repository;
import com.qamqormed.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    List<Specialty> findAllByOrderByNameAsc();
    List<Specialty> findAllByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    java.util.Optional<Specialty> findByNameIgnoreCase(String name);
    java.util.Optional<Specialty> findByCodeIgnoreCase(String code);
}
