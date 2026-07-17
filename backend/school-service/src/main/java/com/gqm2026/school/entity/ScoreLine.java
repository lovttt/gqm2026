package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 高中录取分数线（按年）。当前仅统招线（TONGZHAO）。
 * 用于志愿模拟器「理性考生」冲/稳/保 分档的参考依据（考生报志愿看去年线判断够不够得着）。
 */
@Entity
@Table(name = "score_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 高中 id */
    private Long highSchoolId;

    /** 年份，如 2025 */
    private int year;

    /** 批次：目前仅统招 TONGZHAO */
    @Builder.Default
    private String batch = "TONGZHAO";

    /** 该高中该年录取分数线（510 量纲） */
    private int score;
}
