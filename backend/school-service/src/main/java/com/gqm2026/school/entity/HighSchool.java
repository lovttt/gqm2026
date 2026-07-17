package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "high_school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HighSchool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** 教委官方学校代码（如 101001=二中，201017=龙潭），1开头=重点校，2开头=普通校 */
    private String code;

    @Builder.Default
    private String district = "东城区";

    /** 统招招生计划数 */
    private int tongzhaoQuota;

    /** 校额到校录取数（招生计划口径，可在高中管理页编辑；与 quotaSeats 实际分配池相互独立） */
    @Builder.Default
    private int quotaAdmitted = 0;

    /** 学校层次：KEY=重点(优质高中) / NORMAL=普通。用于志愿模拟的「学校层次偏好」因子 */
    @Builder.Default
    private String tier = "NORMAL";

    /** 所属片区(东城区内 1~N)，用于志愿模拟的「区域/离家距离」因子，距离≈|初中校片区-高中片区| */
    @Builder.Default
    private int zone = 1;
}
