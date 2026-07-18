package com.gqm2026.student.application;

import com.gqm2026.student.dto.ApplicationView;
import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.QuotaEligibilityService;
import com.gqm2026.student.simulator.ApplicationSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 志愿应用服务：查询/增删改/提交锁/整页保存/模拟/校额资格重算。
 * 提交锁与「不存在资源」以 ResponseStatusException 表达（错误体含 message，匹配前端契约）。
 */
@Service
@RequiredArgsConstructor
public class ApplicationAppService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final ApplicationSimulator simulator;
    private final QuotaEligibilityService quotaEligibilityService;

    @Transactional(readOnly = true)
    public Page<ApplicationView> listApplications(Long studentId, String batch, String studentName,
                                                  Long juniorSchoolId, Pageable pageable) {
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
            if (scopedIds.isEmpty()) return Page.empty(pageable);
            spec = spec.and((root, q, cb) -> root.get("studentId").in(scopedIds));
        }
        if (studentName != null && !studentName.isBlank()) {
            List<Long> nameMatched = studentRepository.findByNameContaining(studentName).stream()
                    .map(Student::getId).collect(Collectors.toList());
            if (nameMatched.isEmpty()) return Page.empty(pageable);
            final List<Long> matchedIds = nameMatched;
            spec = spec.and((root, q, cb) -> root.get("studentId").in(matchedIds));
        }
        Page<Application> page = applicationRepository.findAll(spec, pageable);
        List<ApplicationView> views = page.getContent().stream().map(this::toView).collect(Collectors.toList());
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

    @Transactional
    public Student submit(Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "考生不存在"));
        s.submit();
        return studentRepository.save(s);
    }

    @Transactional
    public Student reopen(Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "考生不存在"));
        s.unsubmit();
        return studentRepository.save(s);
    }

    private void ensureEditable(Long studentId) {
        studentRepository.findById(studentId).ifPresent(s -> {
            if (s.isSubmitted()) {
                throw new ResponseStatusException(BAD_REQUEST, "该考生志愿已提交锁定，不可修改");
            }
        });
    }

    @Transactional
    public Application createApplication(Application application) {
        ensureEditable(application.getStudentId());
        application.setId(null);
        return applicationRepository.save(application);
    }

    @Transactional
    public Map<String, Object> simulateApplications() {
        int generated = simulator.regenerateAll();
        return Map.of("generated", generated);
    }

    @Transactional
    public Map<String, Object> recomputeQuotaEligibility() {
        return quotaEligibilityService.recompute();
    }

    @Transactional
    public List<Application> saveStudentApplications(Long studentId, List<Application> applications) {
        ensureEditable(studentId);
        applicationRepository.deleteByStudentId(studentId);
        applications.forEach(a -> {
            a.setId(null);
            a.setStudentId(studentId);
        });
        return applicationRepository.saveAll(applications);
    }

    @Transactional
    public Application updateApplication(Long id, Application application) {
        Application existing = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "志愿不存在"));
        ensureEditable(existing.getStudentId());
        application.setId(id);
        return applicationRepository.save(application);
    }

    @Transactional
    public void deleteApplication(Long id) {
        Application existing = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "志愿不存在"));
        ensureEditable(existing.getStudentId());
        applicationRepository.deleteById(id);
    }
}
