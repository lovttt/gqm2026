package com.gqm2026.student.application;

import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.StudentGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考生种子数据应用服务。首次启动按班数生成考生；已存在则跳过。
 * CommandLineRunner（SeedDataService）仅委托本服务。
 */
@Service
@RequiredArgsConstructor
public class SeedDataAppService {

    private final StudentRepository studentRepository;
    private final StudentGenerator studentGenerator;

    @Transactional
    public void seedIfEmpty() {
        if (!studentRepository.findAll().isEmpty()) {
            return;
        }
        studentGenerator.generate();
    }
}
