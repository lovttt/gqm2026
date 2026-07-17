package com.gqm2026.student.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验问题条目。
 *
 * <p>level 含义：
 * <ul>
 *   <li>BLOCK：阻断性校验失败，对应批次志愿入口被锁定或参数不合法，无法继续</li>
 *   <li>WARN：提示性（如志愿被截断至批次上限、贯通项被屏蔽）</li>
 *   <li>INFO：信息性（如某些学校因通勤/梯队被过滤）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationIssue {

    /** 校验项代码，如 QUOTA_THRESHOLD / GUANTONG_THRESHOLD / WEIGHT_SUM / CAPACITY */
    private String code;

    /** BLOCK / WARN / INFO */
    private String level;

    /** 可直接展示给用户的提示文案 */
    private String message;

    /** 关联批次（QUOTA/TONGZHAO/GUANTONG/全局），可空 */
    private String batch;
}
