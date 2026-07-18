package com.gqm2026.student.application;

import com.gqm2026.student.dto.StudentDataset;
import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 考生/志愿导入导出应用服务。
 */
@Service
@RequiredArgsConstructor
public class StudentDatasetAppService {

    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public StudentDataset exportDataset() {
        return StudentDataset.builder()
                .students(studentRepository.findAll())
                .applications(applicationRepository.findAll())
                .build();
    }

    @Transactional
    public String importDataset(StudentDataset dataset) {
        applicationRepository.deleteAll();
        studentRepository.deleteAll();
        if (dataset.getStudents() != null) {
            dataset.getStudents().forEach(s -> s.setId(null));
            studentRepository.saveAll(dataset.getStudents());
        }
        if (dataset.getApplications() != null) {
            dataset.getApplications().forEach(a -> a.setId(null));
            applicationRepository.saveAll(dataset.getApplications());
        }
        return "imported";
    }

    /** CSV 批量导入考生：name,juniorSchoolId,chinese,math,english,physics,politics,pe,eligibility */
    @Transactional
    public String importCsv(String csv) {
        String[] lines = csv.split("\\r?\\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] f = line.split(",");
            if (f.length < 9) continue;
            Student s = Student.builder()
                    .name(f[0].trim())
                    .juniorSchoolId(Long.parseLong(f[1].trim()))
                    .chinese(Integer.parseInt(f[2].trim()))
                    .math(Integer.parseInt(f[3].trim()))
                    .english(Integer.parseInt(f[4].trim()))
                    .physics(Integer.parseInt(f[5].trim()))
                    .politics(Integer.parseInt(f[6].trim()))
                    .pe(Integer.parseInt(f[7].trim()))
                    .hasQuotaEligibility(Boolean.parseBoolean(f[8].trim()) || "1".equals(f[8].trim()))
                    .comprehensiveEval("B")
                    .crossDistrict(false)
                    .ticketNo("T" + System.currentTimeMillis() + "-" + f[0].trim())
                    .build();
            studentRepository.save(s);
        }
        return "csv-imported";
    }
}
