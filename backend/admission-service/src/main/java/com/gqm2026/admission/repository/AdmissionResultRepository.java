package com.gqm2026.admission.repository;

import com.gqm2026.admission.entity.AdmissionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdmissionResultRepository extends JpaRepository<AdmissionResult, Long>, JpaSpecificationExecutor<AdmissionResult> {
    List<AdmissionResult> findByStudentId(Long studentId);

    List<AdmissionResult> findByBatch(String batch);

    List<AdmissionResult> findByRunId(Long runId);

    Page<AdmissionResult> findByRunId(Long runId, Pageable pageable);

    /** 返回最新一次模拟运行的 runId（无数据返回 null） */
    @Query("select max(r.runId) from AdmissionResult r")
    Long findLatestRunId();

    void deleteByBatch(String batch);

    void deleteByRunId(Long runId);
}
