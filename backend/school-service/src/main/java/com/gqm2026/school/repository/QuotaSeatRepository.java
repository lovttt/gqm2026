package com.gqm2026.school.repository;

import com.gqm2026.school.entity.QuotaSeat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotaSeatRepository extends JpaRepository<QuotaSeat, Long> {
    Page<QuotaSeat> findByJuniorSchoolId(Long juniorSchoolId, Pageable pageable);

    List<QuotaSeat> findByJuniorSchoolIdIn(List<Long> juniorSchoolIds);

    Page<QuotaSeat> findByHighSchoolId(Long highSchoolId, Pageable pageable);

    Page<QuotaSeat> findByJuniorSchoolIdAndHighSchoolId(Long juniorSchoolId, Long highSchoolId, Pageable pageable);

    List<QuotaSeat> findByJuniorSchoolId(Long juniorSchoolId);

    List<QuotaSeat> findByHighSchoolId(Long highSchoolId);

    QuotaSeat findByJuniorSchoolIdAndHighSchoolId(Long juniorSchoolId, Long highSchoolId);
}
