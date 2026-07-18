package com.gqm2026.student.application;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.StudentGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 考生应用服务：CRUD + 按班数生成考生。业务规则内聚于实体/领域服务，本服务仅编排。
 */
@Service
@RequiredArgsConstructor
public class StudentAppService {

    private final StudentRepository studentRepository;
    private final StudentGenerator studentGenerator;

    @Transactional(readOnly = true)
    public Page<Student> listStudents(Long juniorSchoolId, Integer minTotal, Integer maxTotal,
                                      Boolean quotaEligibility, Pageable pageable) {
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

    @Transactional
    public int generate(int perClass) {
        return studentGenerator.generate(perClass);
    }

    @Transactional(readOnly = true)
    public Page<Student> listByJuniorSchool(Long juniorSchoolId, Pageable pageable) {
        return studentRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
    }

    @Transactional
    public Student createStudent(Student student) {
        student.setId(null);
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student student) {
        student.setId(id);
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
}
