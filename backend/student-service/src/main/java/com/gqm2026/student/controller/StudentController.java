package com.gqm2026.student.controller;

import com.gqm2026.student.application.StudentAppService;
import com.gqm2026.student.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentAppService studentAppService;

    /** 考生列表（分页）+ 可选筛选：初中校、总分区间、校额资格 */
    @GetMapping("/students")
    public Page<Student> listStudents(
            @RequestParam(required = false) Long juniorSchoolId,
            @RequestParam(required = false) Integer minTotal,
            @RequestParam(required = false) Integer maxTotal,
            @RequestParam(required = false) Boolean quotaEligibility,
            Pageable pageable) {
        return studentAppService.listStudents(juniorSchoolId, minTotal, maxTotal, quotaEligibility, pageable);
    }

    /** 按 2023 小升初各班数 × 班额 重新生成全部考生（先清空旧考生与志愿）。perClass 默认 40 */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "40") int perClass) {
        int n = studentAppService.generate(perClass);
        Map<String, Object> m = new HashMap<>();
        m.put("generated", n);
        m.put("perClass", perClass);
        return m;
    }

    @GetMapping("/students/junior/{juniorSchoolId}")
    public Page<Student> listByJuniorSchool(@PathVariable Long juniorSchoolId, Pageable pageable) {
        return studentAppService.listByJuniorSchool(juniorSchoolId, pageable);
    }

    @PostMapping("/students")
    public Student createStudent(@RequestBody Student student) {
        return studentAppService.createStudent(student);
    }

    @PutMapping("/students/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return studentAppService.updateStudent(id, student);
    }

    @DeleteMapping("/students/{id}")
    public void deleteStudent(@PathVariable Long id) {
        studentAppService.deleteStudent(id);
    }
}
