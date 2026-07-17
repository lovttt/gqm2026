package com.gqm2026.student.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 偏好权重联动对比：以「上一轮权重」重算结果对比「本轮权重」结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanComparison {

    /** 对比的批次 */
    private String batch;

    /** 上一轮权重下的志愿顺序（高中 id） */
    private List<Long> before;

    /** 本轮权重下的志愿顺序（高中 id） */
    private List<Long> after;

    /** 本轮新增的高中 id */
    private List<Long> added;

    /** 本轮移除的高中 id */
    private List<Long> removed;

    /** 两轮都在但顺序发生变化的高中 id */
    private List<Long> reordered;
}
