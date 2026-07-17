package com.gqm2026.student.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "batch", "priority"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    /** 批次：QUOTA=校额到校，TONGZHAO=统招 */
    private String batch;

    /** 志愿序号，从 1 开始 */
    private int priority;

    /** 填报的高中 id */
    private Long highSchoolId;
}
