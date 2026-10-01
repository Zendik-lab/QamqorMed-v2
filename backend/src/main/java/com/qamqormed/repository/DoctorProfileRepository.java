package com.qamqormed.repository;
import com.qamqormed.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long>, JpaSpecificationExecutor<DoctorProfile> {
    Optional<DoctorProfile> findByUserId(Long userId);
    @Query("select distinct d from DoctorProfile d join fetch d.user u join fetch d.specialty s join fetch d.clinic c join fetch c.city city join fetch city.region r order by u.lastName, u.firstName")
    List<DoctorProfile> findAllWithDetails();
}
