package com.gqm2026.admission.application;

import com.gqm2026.admission.dto.SchoolSnapshot;
import com.gqm2026.admission.dto.StudentSnapshot;
import com.gqm2026.admission.engine.AdmissionEngine;
import com.gqm2026.admission.entity.AdmissionResult;
import com.gqm2026.admission.entity.AdmissionStatus;
import com.gqm2026.admission.infrastructure.acl.SchoolSnapshotPort;
import com.gqm2026.admission.infrastructure.acl.StudentSnapshotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 录取应用服务（聚合 AdmissionResult）：编排录取用例。
 *
 * <p>关键边界（G7 / 09 §2.5）：跨服务快照拉取（HTTP）在事务<b>外</b>经 {@link SchoolSnapshotPort}/
 * {@link StudentSnapshotPort} 完成，再交由 {@link AdmissionEngine}（标注 {@code @Transactional}）做纯计算 + 落库，
 * 避免长事务内持有 HTTP 连接。查询类方法为只读事务。
 */
@Service
@RequiredArgsConstructor
public class AdmissionAppService {

    private final SchoolSnapshotPort schoolSnapshotPort;
    private final StudentSnapshotPort studentSnapshotPort;
    private final AdmissionEngine admissionEngine;

    /** 一键顺序模拟：先经 ACL 端口拉快照（TX 外），再调引擎（TX 内）计算落库 */
    public Map<String, Object> runFull() {
        SchoolSnapshot ss = schoolSnapshotPort.fetch();
        StudentSnapshot st = studentSnapshotPort.fetch();
        return admissionEngine.runFull(ss, st);
    }

    public Map<String, Object> runQuotaOnly() {
        SchoolSnapshot ss = schoolSnapshotPort.fetch();
        StudentSnapshot st = studentSnapshotPort.fetch();
        return admissionEngine.runQuotaOnly(ss, st);
    }

    public Map<String, Object> runTongzhaoOnly() {
        SchoolSnapshot ss = schoolSnapshotPort.fetch();
        StudentSnapshot st = studentSnapshotPort.fetch();
        return admissionEngine.runTongzhaoOnly(ss, st);
    }

    @Transactional(readOnly = true)
    public Page<AdmissionResult> results(Pageable pageable, Long juniorSchoolId, Long highSchoolId,
                                          Integer minScore, Integer maxScore, String status, String studentName) {
        return admissionEngine.results(pageable, juniorSchoolId, highSchoolId, minScore, maxScore,
                AdmissionStatus.from(status), studentName);
    }

    @Transactional(readOnly = true)
    public List<AdmissionResult> resultsByStudent(Long studentId) {
        return admissionEngine.resultsByStudent(studentId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentStats() {
        return admissionEngine.currentStats();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> runs() {
        return admissionEngine.runs();
    }

    @Transactional(readOnly = true)
    public List<AdmissionResult> resultsByRun(Long runId) {
        return admissionEngine.resultsByRun(runId);
    }

    /** 按高中聚合最近一次模拟运行：拉 school 快照取计划数后聚合 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> summaryBySchool() {
        SchoolSnapshot ss = schoolSnapshotPort.fetch();
        return admissionEngine.summaryBySchool(ss);
    }
}
