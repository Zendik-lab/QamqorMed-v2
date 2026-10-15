package com.qamqormed.repository;

import com.qamqormed.entity.ICD10Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICD10CodeRepository extends JpaRepository<ICD10Code, Long> {
    Optional<ICD10Code> findByCodeIgnoreCase(String code);
    @Query("select c from ICD10Code c where c.active = true and c.selectable = true and (lower(c.code) like lower(concat('%', :query, '%')) or lower(coalesce(c.titleRu, c.name)) like lower(concat('%', :query, '%')) or lower(coalesce(c.titleKk, '')) like lower(concat('%', :query, '%')) or lower(coalesce(c.titleEn, '')) like lower(concat('%', :query, '%'))) order by case when lower(c.code) = lower(:query) then 0 when lower(c.code) like lower(concat(:query, '%')) then 1 else 2 end, c.code")
    Page<ICD10Code> search(@Param("query") String query, Pageable pageable);
}
