package com.qamqormed.repository;
import jakarta.persistence.LockModeType;
import com.qamqormed.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long>, JpaSpecificationExecutor<DoctorProfile> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DoctorProfile d where d.id = :doctorId")
    Optional<DoctorProfile> findByIdForUpdate(@Param("doctorId") Long doctorId);
    Optional<DoctorProfile> findByUserId(Long userId);
    Optional<DoctorProfile> findBySeedKey(String seedKey);
    @Query("select distinct d from DoctorProfile d join fetch d.user u join fetch d.specialty s join fetch d.clinic c join fetch c.city city join fetch city.region r order by u.lastName, u.firstName")
    List<DoctorProfile> findAllWithDetails();
    long countByDemoTrue();
    long countByDemoTrueAndActiveTrue();
    @Query("select distinct s from DoctorProfile d join d.specialty s join d.clinic c join c.city city join city.region r where d.active = true and d.user.enabled = true and s.active = true and c.active = true and city.active = true and r.active = true and city.id = :cityId order by s.name")
    List<com.qamqormed.entity.Specialty> findActiveSpecialtiesByCityId(Long cityId);
}
