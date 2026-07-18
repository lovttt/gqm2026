package com.gqm2026.student.repository;

import com.gqm2026.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    Page<Student> findByJuniorSchoolId(Long juniorSchoolId, Pageable pageable);

    List<Student> findByJuniorSchoolId(Long juniorSchoolId);

    List<Student> findByNameContaining(String name);

    List<Student> findByHasQuotaEligibilityTrue();

    List<Student> findBySubmittedFalse();
}
