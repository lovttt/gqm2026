package com.gqm2026.school.repository;

import com.gqm2026.school.entity.ScoreLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreLineRepository extends JpaRepository<ScoreLine, Long> {
    List<ScoreLine> findByYear(int year);
}
