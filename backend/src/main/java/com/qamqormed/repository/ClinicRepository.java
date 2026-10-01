package com.qamqormed.repository;
import com.qamqormed.entity.Clinic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    @Query("select c from Clinic c join fetch c.city city join fetch city.region r where c.active = true and city.active = true and r.active = true order by c.name")
    List<Clinic> findAllPublic();
    @Query(value = "select c from Clinic c join c.city city join city.region r where c.active = true and city.active = true and r.active = true order by c.name",
            countQuery = "select count(c) from Clinic c where c.active = true and c.city.active = true and c.city.region.active = true")
    Page<Clinic> findPagePublic(Pageable pageable);
    @Query("select c from Clinic c join fetch c.city city join fetch city.region r where c.city.id = :cityId order by c.name")
    List<Clinic> findAllByCityId(Long cityId);
    @Query("select c from Clinic c join fetch c.city city join fetch city.region r where c.city.id = :cityId and c.active = true and city.active = true and r.active = true order by c.name")
    List<Clinic> findPublicByCityId(Long cityId);
    @Query("select c from Clinic c join fetch c.city city join fetch city.region r where c.city.region.id = :regionId order by c.name")
    List<Clinic> findAllByRegionId(Long regionId);
    @Query("select c from Clinic c join fetch c.city city join fetch city.region r where c.city.region.id = :regionId and c.active = true and city.active = true and r.active = true order by c.name")
    List<Clinic> findPublicByRegionId(Long regionId);
    @Query("select distinct c from Clinic c join fetch c.city city join fetch city.region r where c.id in (select d.clinic.id from DoctorProfile d where d.active = true and d.user.enabled = true and d.specialty.id = :specialtyId) and c.city.id = :cityId and c.active = true and city.active = true and r.active = true order by c.name")
    List<Clinic> findPublicByCityIdAndSpecialtyId(Long cityId, Long specialtyId);
    java.util.Optional<Clinic> findByNameIgnoreCaseAndCityId(String name, Long cityId);
    java.util.Optional<Clinic> findByExternalSourceId(String externalSourceId);
    long countByRealTrue();
}
