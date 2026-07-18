package com.gqm2026.student.controller;

import com.gqm2026.student.application.GeneratorAppService;
import com.gqm2026.student.generator.dto.GenerateRequest;
import com.gqm2026.student.generator.dto.GenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 单考生志愿生成器接口（见用户校验清单）。
 *
 * <p>POST /student/generator/generate：传入考生 id 与交互参数，返回校验问题 + 各批次志愿方案
 * + 通勤/梯队过滤信息 + 偏好权重联动前后对比。
 */
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class GeneratorController {

    private final GeneratorAppService generatorAppService;

    @PostMapping("/generator/generate")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        return generatorAppService.generate(request);
    }
}
