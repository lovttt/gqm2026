package com.gqm2026.student.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 志愿生成结果：含校验问题、各批次方案、过滤信息、权重联动对比。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateResponse {

    // —— 考生快照 ——
    private Long studentId;
    private String studentName;
    private int totalScore;
    private Long juniorSchoolId;
    private int juniorZone;
    private String comprehensiveEval;
    private boolean quotaEligible;

    // —— 校验 ——
    private List<ValidationIssue> issues;

    // —— 各批次方案（已按批次容量截断、按偏好权重排序） ——
    private List<GeneratedChoice> quotaPlan;
    private List<GeneratedChoice> tongzhaoPlan;
    private List<GeneratedChoice> guantongPlan;

    /** 贯通志愿是否被屏蔽（总分<380） */
    private boolean guantongHidden;

    // —— 过滤信息 ——
    /** 因超过通勤上限被过滤掉的高中 id */
    private List<Long> filteredByCommute;
    /** 因不匹配高考出口梯队偏好被过滤掉的高中 id */
    private List<Long> filteredByGaokaoTier;

    // —— 偏好权重联动对比（仅当请求提供 prev* 权重时非空） ——
    private List<PlanComparison> comparisons;
}
