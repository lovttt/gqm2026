package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 一分一段表（按年）。year 区分 2025（历史对照）与 2026（生成全量考生的依据）。
 * headcount = 该分数段当前人数（每 1 分段人数），用于按分布抽样生成考生；
 * cumulative = 累计人数（≥该分数的人数），可选，用于校验。
 */
@Entity
@Table(name = "score_segment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 年份，如 2025 / 2026 */
    private int year;

    /** 分数（510 量纲），区间 430~510 */
    private int score;

    /** 该分数段当前人数（每 1 分段人数） */
    private int headcount;

    /** 累计人数（≥该分数的人数），可选，用于校验 */
    private Integer cumulative;
}
