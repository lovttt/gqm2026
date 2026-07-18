package com.gqm2026.student.controller;

import com.gqm2026.student.application.ApplicationAppService;
import com.gqm2026.student.dto.ApplicationView;
import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationAppService applicationAppService;

    /** 志愿填报查询（分页 + 考生姓名/初中校/批次过滤），返回附带名称的 ApplicationView */
    @GetMapping("/applications")
    public Page<ApplicationView> listApplications(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) Long juniorSchoolId,
            Pageable pageable) {
        return applicationAppService.listApplications(studentId, batch, studentName, juniorSchoolId, pageable);
    }

    /** 提交志愿：锁定该考生 */
    @PostMapping("/students/{id}/submit")
    public Student submit(@PathVariable Long id) {
        return applicationAppService.submit(id);
    }

    /** 撤回提交：重新开放志愿编辑 */
    @PostMapping("/students/{id}/reopen")
    public Student reopen(@PathVariable Long id) {
        return applicationAppService.reopen(id);
    }

    @PostMapping("/applications")
    public Application createApplication(@RequestBody Application application) {
        return applicationAppService.createApplication(application);
    }

    /** 按「理性考生」策略为所有未提交锁定的考生重新模拟生成志愿 */
    @PostMapping("/applications/simulate")
    public Map<String, Object> simulateApplications() {
        return applicationAppService.simulateApplications();
    }

    /** 结合各初中校名额总数重算校额资格 */
    @PostMapping("/quota-eligibility/recompute")
    public Map<String, Object> recomputeQuotaEligibility() {
        return applicationAppService.recomputeQuotaEligibility();
    }

    /** 一次性保存某考生的全部志愿（先清空再写入） */
    @PostMapping("/applications/student/{studentId}")
    public List<Application> saveStudentApplications(@PathVariable Long studentId,
                                                     @RequestBody List<Application> applications) {
        return applicationAppService.saveStudentApplications(studentId, applications);
    }

    @PutMapping("/applications/{id}")
    public Application updateApplication(@PathVariable Long id, @RequestBody Application application) {
        return applicationAppService.updateApplication(id, application);
    }

    @DeleteMapping("/applications/{id}")
    public void deleteApplication(@PathVariable Long id) {
        applicationAppService.deleteApplication(id);
    }
}
