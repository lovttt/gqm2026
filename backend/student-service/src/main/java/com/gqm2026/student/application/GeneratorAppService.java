package com.gqm2026.student.application;

import com.gqm2026.student.generator.GeneratorService;
import com.gqm2026.student.generator.dto.GenerateRequest;
import com.gqm2026.student.generator.dto.GenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 单考生志愿生成器应用服务（无持久化，纯计算）。委托领域 GeneratorService。
 */
@Service
@RequiredArgsConstructor
public class GeneratorAppService {

    private final GeneratorService generatorService;

    public GenerateResponse generate(GenerateRequest request) {
        return generatorService.generate(request);
    }
}
