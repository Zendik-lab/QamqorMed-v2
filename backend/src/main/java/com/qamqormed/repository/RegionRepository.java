package com.qamqormed.repository;
import com.qamqormed.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByOrderByNameAsc();
    List<Region> findAllByActiveTrueOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Region> findByCodeIgnoreCase(String code);
}
