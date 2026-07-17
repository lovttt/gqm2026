package com.gqm2026.student.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单考生志愿生成请求（交互式参数）。
 *
 * <p>用户的设定参数最终都要经过 {@code GeneratorService} 的校验清单，
 * 不合法的组合会返回拦截提示而非静默忽略。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateRequest {

    /** 目标考生 id（必填） */
    private Long studentId;

    /** 家校通勤时长上限（分钟）；null 表示不限制 */
    private Integer commuteCapMinutes;

    /** 高考出口梯队偏好：TOP / HEAD / MID（null=不按梯队过滤） */
    private String gaokaoTierPref;

    /** 是否包含跨区投放东城计划的学校（占位：当前数据无跨区标记） */
    @Builder.Default
    private boolean includeCrossDistrict = false;

    /** 梯度权重：冲刺 / 稳妥 / 兜底，三者之和须为 100 */
    @Builder.Default
    private int sprintWeight = 30;
    @Builder.Default
    private int steadyWeight = 40;
    @Builder.Default
    private int safetyWeight = 30;

    /** 偏好权重：通勤距离（0~100） */
    @Builder.Default
    private int commuteWeight = 50;
    /** 偏好权重：高考出口（0~100） */
    @Builder.Default
    private int gaokaoOutputWeight = 50;

    /**
     * 考生综合素质评价等级（覆盖占位默认值）。
     * 用于演示校额到校门槛拦截；null 时取占位默认 "B"（达标）。
     */
    private String comprehensiveEval;

    /**
     * 偏好权重联动对比：上一轮的通勤/高考出口权重。
     * 提供时后端以「旧权重」重算并与「新权重」结果做前后对比。
     */
    private Integer prevCommuteWeight;
    private Integer prevGaokaoOutputWeight;
}
