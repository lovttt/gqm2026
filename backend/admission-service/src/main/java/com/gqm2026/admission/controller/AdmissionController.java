package com.gqm2026.admission.controller;

import com.gqm2026.admission.application.AdmissionAppService;
import com.gqm2026.admission.entity.AdmissionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admission")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionAppService admissionAppService;

    /** 一键顺序模拟：校额到校 -> 统招 */
    @PostMapping("/run/full")
    public Map<String, Object> runFull() {
        return admissionAppService.runFull();
    }

    /** 仅跑校额到校批次 */
    @PostMapping("/run/quota")
    public Map<String, Object> runQuota() {
        return admissionAppService.runQuotaOnly();
    }

    /** 仅跑统招批次（以已录取的校额到校考生为豁免） */
    @PostMapping("/run/tongzhao")
    public Map<String, Object> runTongzhao() {
        return admissionAppService.runTongzhaoOnly();
    }

    @GetMapping("/results")
    public Page<AdmissionResult> results(Pageable pageable,
                                          @RequestParam(required = false) Long juniorSchoolId,
                                          @RequestParam(required = false) Long highSchoolId,
                                          @RequestParam(required = false) Integer minScore,
                                          @RequestParam(required = false) Integer maxScore,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String studentName) {
        return admissionAppService.results(pageable, juniorSchoolId, highSchoolId, minScore, maxScore, status, studentName);
    }

    @GetMapping("/results/student/{studentId}")
    public List<AdmissionResult> resultsByStudent(@PathVariable Long studentId) {
        return admissionAppService.resultsByStudent(studentId);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return admissionAppService.currentStats();
    }

    /** 列出全部历史模拟运行（含每轮统计），用于多次模拟对比 */
    @GetMapping("/runs")
    public List<Map<String, Object>> runs() {
        return admissionAppService.runs();
    }

    /** 查看某一次模拟运行的录取结果 */
    @GetMapping("/runs/{runId}")
    public List<AdmissionResult> resultsByRun(@PathVariable Long runId) {
        return admissionAppService.resultsByRun(runId);
    }

    /** 按高中聚合最近一次模拟运行的录取情况（各校计划/录取/分数线/满额率） */
    @GetMapping("/results/summary-by-school")
    public List<Map<String, Object>> summaryBySchool() {
        return admissionAppService.summaryBySchool();
    }
}
