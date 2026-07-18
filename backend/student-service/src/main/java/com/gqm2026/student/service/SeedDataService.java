package com.gqm2026.student.service;

import com.gqm2026.student.application.SeedDataAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 考生种子数据引导器（基础设施）：仅委托 SeedDataAppService，保持应用层与启动器分离。
 */
@Component
@RequiredArgsConstructor
public class SeedDataService implements CommandLineRunner {

    private final SeedDataAppService seedDataAppService;

    @Override
    public void run(String... args) {
        seedDataAppService.seedIfEmpty();
    }
}
