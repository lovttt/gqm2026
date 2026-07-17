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

    @PrePersist
    @PreUpdate
    public void computeTotal() {
        this.totalScore = chinese + math + english + physics + politics + pe;
    }
}
