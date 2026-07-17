package com.gqm2026.student.repository;

import com.gqm2026.student.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    Page<Application> findByStudentId(Long studentId, Pageable pageable);

    Page<Application> findByStudentIdAndBatch(Long studentId, String batch, Pageable pageable);

    Page<Application> findByBatch(String batch, Pageable pageable);

    void deleteByStudentId(Long studentId);
}
