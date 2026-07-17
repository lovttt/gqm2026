package com.gqm2026.student.controller;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.StudentGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentRepository studentRepository;
    private final StudentGenerator studentGenerator;

    /** 考生列表（分页）+ 可选筛选：初中校、总分区间、校额资格 */
    @GetMapping("/students")
    public Page<Student> listStudents(
            @RequestParam(required = false) Long juniorSchoolId,
            @RequestParam(required = false) Integer minTotal,
            @RequestParam(required = false) Integer maxTotal,
            @RequestParam(required = false) Boolean quotaEligibility,
            Pageable pageable) {
        Specification<Student> spec = Specification.where(null);
        if (juniorSchoolId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("juniorSchoolId"), juniorSchoolId));
        }
        if (minTotal != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("totalScore"), minTotal));
        }
        if (maxTotal != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("totalScore"), maxTotal));
        }
        if (quotaEligibility != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("hasQuotaEligibility"), quotaEligibility));
        }
        return studentRepository.findAll(spec, pageable);
    }

    /** 按 2023 小升初各班数 × 班额 重新生成全部考生（先清空旧考生与志愿）。perClass 默认 40 */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "40") int perClass) {
        int n = studentGenerator.generate(perClass);
        Map<String, Object> m = new HashMap<>();
        m.put("generated", n);
        m.put("perClass", perClass);
        return m;
    }

    @GetMapping("/students/junior/{juniorSchoolId}")
    public Page<Student> listByJuniorSchool(@PathVariable Long juniorSchoolId, Pageable pageable) {
        return studentRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
    }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) {
        student.setId(null);
        return studentRepository.save(student);
    }

    @PutMapping("/students/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        return studentRepository.save(student);
    }

    @DeleteMapping("/students/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentRepository.deleteById(id);
    }
}
