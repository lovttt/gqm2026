package com.gqm2026.school.repository;

import com.gqm2026.school.entity.JuniorSchool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuniorSchoolRepository extends JpaRepository<JuniorSchool, Long> {
    JuniorSchool findByName(String name);
}
