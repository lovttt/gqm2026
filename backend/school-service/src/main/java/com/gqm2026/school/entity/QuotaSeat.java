package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quota_seat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"junior_school_id", "high_school_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 初中校 id */
    private Long juniorSchoolId;

    /** 高中 id */
    private Long highSchoolId;

    /** 该初中校分配到该高中的校额到校名额 */
    private int quota;

    /** 合并展示用的初中校名称（如「东直门中学 / 第一六五中学」）；不参与持久化 */
    @Transient
    private String juniorSchoolNames;
}
