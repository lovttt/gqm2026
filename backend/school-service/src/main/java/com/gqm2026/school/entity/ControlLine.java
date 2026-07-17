package com.gqm2026.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "control_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 批次类型：QUOTA=校额到校全区最低控制线 */
    private String type;

    /** 控制线分数 */
    private int value;
}
