package com.qamqormed.repository;
import com.qamqormed.entity.NurseProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface NurseProfileRepository extends JpaRepository<NurseProfile, Long>, JpaSpecificationExecutor<NurseProfile> {
    Optional<NurseProfile> findByUserId(Long userId);
    Optional<NurseProfile> findBySeedKey(String seedKey);
    @Query("select distinct n from NurseProfile n join fetch n.user u join fetch n.clinic c join fetch c.city city join fetch city.region r order by u.lastName, u.firstName")
    List<NurseProfile> findAllWithDetails();
    long countByDemoTrue();
    long countByDemoTrueAndActiveTrue();
}
