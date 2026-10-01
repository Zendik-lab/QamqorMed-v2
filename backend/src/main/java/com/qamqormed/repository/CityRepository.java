package com.qamqormed.repository;
import com.qamqormed.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findAllByOrderByNameAsc();
    @Query("select c from City c join fetch c.region r where c.active = true and r.active = true order by c.name")
    List<City> findAllPublic();
    @Query("select c from City c join fetch c.region r where c.region.id = :regionId order by c.name")
    List<City> findAllByRegionId(Long regionId);
    @Query("select c from City c join fetch c.region r where c.region.id = :regionId and c.active = true and r.active = true order by c.name")
    List<City> findPublicByRegionId(Long regionId);
    Optional<City> findByNameIgnoreCaseAndRegionId(String name, Long regionId);
    Optional<City> findByCodeIgnoreCase(String code);
}
