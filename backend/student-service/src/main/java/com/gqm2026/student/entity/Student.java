package com.gqm2026.student.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** 中考准考证号 */
    private String ticketNo;

    /** 来源初中校 id */
    private Long juniorSchoolId;

    private int chinese;
    private int math;
    private int english;
    private int physics;
    private int politics; // 道德与法治
    private int pe;       // 体育

    /** 总分 = 语+数+英+物+道法+体育 */
    private int totalScore;

    /** 是否具备校额到校资格 */
    private boolean hasQuotaEligibility;

    /** 志愿是否已提交锁定：提交后不可再增删改志愿 */
    private boolean submitted = false;

    /**
     * 综合素质评价等级（G7-Q3）：A/B/C/D。
     * 仅 A/B（达标）具备校额到校门槛；C/D 校额失格（统招不受影响）。
     * 默认 "B"（达标占位），见 GeneratorConstants.QUOTA_COMP_EVAL_MIN。
     */
    @Builder.Default
    @Column(nullable = true)
    private String comprehensiveEval = "B";

    /** 跨区投放标识（G7-Q4）：占位，不生效（与 02 非目标「跨区招生」一致） */
    @Builder.Default
    private boolean crossDistrict = false;

    @PrePersist
    @PreUpdate
    public void computeTotal() {
        this.totalScore = chinese + math + english + physics + politics + pe;
    }

    /** 校额到校资格（充血）：达资格字段 + 综合素质评价 A/B（C/D 失格，落实 G7-Q3） */
    public boolean eligibleForQuota() {
        return hasQuotaEligibility && isCompEvalAtLeastB();
    }

    private boolean isCompEvalAtLeastB() {
        return comprehensiveEval != null
                && ("A".equalsIgnoreCase(comprehensiveEval) || "B".equalsIgnoreCase(comprehensiveEval));
    }

    /** 提交锁：锁定志愿，禁止增删改 */
    public void submit() {
        this.submitted = true;
    }

    /** 解除提交锁 */
    public void unsubmit() {
        this.submitted = false;
    }
}
