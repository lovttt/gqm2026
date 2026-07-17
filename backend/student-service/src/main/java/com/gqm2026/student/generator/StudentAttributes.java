package com.gqm2026.student.generator;

import com.gqm2026.student.entity.Student;
import org.springframework.stereotype.Component;

/**
 * 考生/学校扩展属性解析（占位实现）。
 *
 * <p>用户清单的若干校验依赖当前数据模型尚未落库的字段：
 * <ul>
 *   <li>考生「综合素质评价」等级（校额到校门槛需要 B 等及以上）</li>
 *   <li>高中「贯通培养项目」标识（贯通门槛需要屏蔽该类学校）</li>
 *   <li>高中「跨区投放东城计划」标识（高考出口优先级校验需要包含跨区校）</li>
 * </ul>
 * 按「先接口占位」决策，这里以合理默认值/空集合返回，保证校验流程完整可跑；
 * 待数据模型补齐后，仅替换下列方法的实现即可接入真实数据。
 */
@Component
public class StudentAttributes {

    /**
     * 读取考生综合素质评价等级。
     * 占位：当前 Student 无该字段，默认返回达标等级 "B"（即默认满足校额门槛的评价要求）。
     * 生成请求 {@code GenerateRequest.comprehensiveEval} 可显式覆盖（用于演示不达标的拦截）。
     */
    public String comprehensiveEval(Student student, String requestOverride) {
        if (requestOverride != null && !requestOverride.isBlank()) return requestOverride;
        return GeneratorConstants.QUOTA_COMP_EVAL_MIN; // 默认达标
    }

    /** 该校是否为贯通培养项目学校。占位：当前无该标识，默认 false（无贯通校）。 */
    public boolean isGuantongSchool(Long highSchoolId) {
        return false;
    }

    /** 该校是否跨区投放东城计划。占位：当前无该标识，默认 false。 */
    public boolean isCrossDistrict(Long highSchoolId) {
        return false;
    }
}
