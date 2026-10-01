package com.qamqormed.repository;

import com.qamqormed.entity.ICD10Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICD10CodeRepository extends JpaRepository<ICD10Code, Long> {
    Optional<ICD10Code> findByCodeIgnoreCase(String code);
    @Query("select c from ICD10Code c where c.active = true and (lower(c.code) like lower(concat('%', :query, '%')) or lower(c.name) like lower(concat('%', :query, '%'))) order by c.code")
    List<ICD10Code> search(@Param("query") String query);
}
