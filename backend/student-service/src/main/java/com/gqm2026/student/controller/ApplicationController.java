package com.gqm2026.student.controller;

import com.gqm2026.student.dto.ApplicationView;
import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.simulator.ApplicationSimulator;
import com.gqm2026.student.service.QuotaEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final ApplicationSimulator simulator;
    private final QuotaEligibilityService quotaEligibilityService;

    /**
     * 志愿填报查询（分页）。除 studentId/batch 外，新增：
     * - studentName：按考生姓名模糊匹配
     * - juniorSchoolId：按来源初中校过滤
     * 返回 ApplicationView（附带考生姓名、初中校名、高中名），按 batch、priority 排序。
     */
    @GetMapping("/applications")
    public Page<ApplicationView> listApplications(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) Long juniorSchoolId,
            Pageable pageable) {
        // 初中校过滤需先定位考生集合
        List<Long> scopedStudentIds = null;
        if (juniorSchoolId != null) {
            scopedStudentIds = studentRepository.findByJuniorSchoolId(juniorSchoolId).stream()
                    .map(Student::getId).collect(Collectors.toList());
        }
        final List<Long> scopedIds = scopedStudentIds;
        Specification<Application> spec = Specification.where(null);
        if (studentId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("studentId"), studentId));
        }
        if (batch != null && !batch.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("batch"), batch));
        }
        if (scopedIds != null) {
            if (scopedIds.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, q, cb) -> root.get("studentId").in(scopedIds));
        }
        if (studentName != null && !studentName.isBlank()) {
            List<Long> nameMatched = studentRepository.findByNameContaining(studentName).stream()
                    .map(Student::getId).collect(Collectors.toList());
            if (nameMatched.isEmpty()) {
                return Page.empty(pageable);
            }
            final List<Long> matchedIds = nameMatched;
            spec = spec.and((root, q, cb) -> root.get("studentId").in(matchedIds));
        }
        Page<Application> page = applicationRepository.findAll(spec, pageable);
        List<ApplicationView> views = page.getContent().stream()
                .map(this::toView).collect(Collectors.toList());
        return new PageImpl<>(views, pageable, page.getTotalElements());
    }

    private ApplicationView toView(Application a) {
        Student s = studentRepository.findById(a.getStudentId()).orElse(null);
        ApplicationView v = new ApplicationView();
        v.setId(a.getId());
        v.setStudentId(a.getStudentId());
        v.setStudentName(s != null ? s.getName() : null);
        v.setJuniorSchoolId(s != null ? s.getJuniorSchoolId() : null);
        v.setBatch(a.getBatch());
        v.setPriority(a.getPriority());
        v.setHighSchoolId(a.getHighSchoolId());
        return v;
    }

    /** 提交志愿：锁定该考生，之后不可再增删改志愿 */
    @PostMapping("/students/{id}/submit")
    public Student submit(@PathVariable Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考生不存在"));
        s.setSubmitted(true);
        return studentRepository.save(s);
    }

    /** 撤回提交：重新开放志愿编辑 */
    @PostMapping("/students/{id}/reopen")
    public Student reopen(@PathVariable Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "考生不存在"));
        s.setSubmitted(false);
        return studentRepository.save(s);
    }

    private void ensureEditable(Long studentId) {
        studentRepository.findById(studentId).ifPresent(s -> {
            if (s.isSubmitted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该考生志愿已提交锁定，不可修改");
            }
        });
    }

    @PostMapping("/applications")
    public Application createApplication(@RequestBody Application application) {
        ensureEditable(application.getStudentId());
        application.setId(null);
        return applicationRepository.save(application);
    }

    /** 按「理性考生」策略为所有未提交锁定的考生重新模拟生成志愿（见 docs/spec/07-simulator.md） */
    @PostMapping("/applications/simulate")
    public Map<String, Object> simulateApplications() {
        int generated = simulator.regenerateAll();
        return Map.of("generated", generated);
    }

    /** 结合各初中校名额总数重算校额资格（按初中校取前 N 名，见 docs/spec/02 §2.6） */
    @PostMapping("/quota-eligibility/recompute")
    public Map<String, Object> recomputeQuotaEligibility() {
        return quotaEligibilityService.recompute();
    }

    /** 一次性保存某考生的全部志愿（先清空再写入），方便前端整页提交 */
    @PostMapping("/applications/student/{studentId}")
    @Transactional
    public List<Application> saveStudentApplications(@PathVariable Long studentId,
                                                    @RequestBody List<Application> applications) {
        ensureEditable(studentId);
        applicationRepository.deleteByStudentId(studentId);
        applications.forEach(a -> {
            a.setId(null);
            a.setStudentId(studentId);
        });
        return applicationRepository.saveAll(applications);
    }

    @PutMapping("/applications/{id}")
    public Application updateApplication(@PathVariable Long id, @RequestBody Application application) {
        Application existing = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "志愿不存在"));
        ensureEditable(existing.getStudentId());
        application.setId(id);
        return applicationRepository.save(application);
    }

    @DeleteMapping("/applications/{id}")
    public void deleteApplication(@PathVariable Long id) {
        Application existing = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "志愿不存在"));
        ensureEditable(existing.getStudentId());
        applicationRepository.deleteById(id);
    }
}
