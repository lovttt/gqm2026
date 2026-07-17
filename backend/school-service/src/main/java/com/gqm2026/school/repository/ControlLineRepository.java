package com.gqm2026.school.repository;

import com.gqm2026.school.entity.ControlLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ControlLineRepository extends JpaRepository<ControlLine, Long> {
    Optional<ControlLine> findByType(String type);
}
