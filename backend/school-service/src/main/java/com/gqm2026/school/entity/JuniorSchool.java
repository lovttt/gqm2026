package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "junior_school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JuniorSchool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Builder.Default
    private String district = "东城区";

    /** 所属片区(东城区内 1~N)，用于志愿模拟的「区域/离家距离」因子 */
    @Builder.Default
    private int zone = 1;

    /** 2023 小升初班级数（每年级班数） */
    @Builder.Default
    @Column(nullable = true)
    private int classCount = 0;

    /** 2023 小升初毕业生人数（= 班数 × 班额） */
    @Builder.Default
    @Column(nullable = true)
    private int gradCount = 0;
}
