package com.gqm2026.school.repository;

import com.gqm2026.school.entity.GaokaoTier;
import com.gqm2026.school.entity.HighSchool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HighSchoolRepository extends JpaRepository<HighSchool, Long> {
    List<HighSchool> findByGaokaoTier(GaokaoTier gaokaoTier);
}
