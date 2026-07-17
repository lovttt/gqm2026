package com.gqm2026.student.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成器产出的单条志愿建议。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedChoice {

    private Long highSchoolId;
    private String highSchoolName;

    /** 批次 QUOTA/TONGZHAO/GUANTONG */
    private String batch;

    /** 学校层次 KEY/NORMAL */
    private String tier;

    /** 高考出口梯队 TOP/HEAD/MID */
    private String gaokaoTier;

    /** 参考分数：该校 2025 统招线（缺则估算值） */
    private Integer referenceScore;

    /** 梯度区间：SPRINT/STEADY/SAFETY/NONE（不在梯度区间内） */
    private String scoreBand;

    /** 参考分与考生总分的分差（正=学校更难） */
    private int delta;

    /** 家校通勤估算（分钟） */
    private int commuteMinutes;

    /** 片区距离 |初中zone-高中zone| */
    private int zoneDistance;

    /** 偏好加权排序分（越大越靠前） */
    private int preferenceScore;
}
