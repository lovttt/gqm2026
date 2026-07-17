package com.gqm2026.student.service;

import com.gqm2026.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeedDataService implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final StudentGenerator studentGenerator;

    @Override
    @Transactional
    public void run(String... args) {
        if (!studentRepository.findAll().isEmpty()) {
            return;
        }
        // 首次启动：按 2023 小升初各班数 × 班额 估算并生成考生（详见 StudentGenerator）
        int n = studentGenerator.generate();
        System.out.println("[student-service] 种子数据初始化完成: 考生=" + n
                + "；志愿请调用 POST /student/applications/simulate 由模拟器生成");
    }
}
