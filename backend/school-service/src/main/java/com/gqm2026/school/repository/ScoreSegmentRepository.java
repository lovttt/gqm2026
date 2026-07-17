package com.gqm2026.school.repository;

import com.gqm2026.school.entity.ScoreSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreSegmentRepository extends JpaRepository<ScoreSegment, Long> {
    List<ScoreSegment> findByYearOrderByScoreDesc(int year);
}
