package com.gqm2026.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 志愿填报查询视图：在 Application 基础上附带考生与高中名称，便于前端直接展示 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationView {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long juniorSchoolId;
    private String juniorSchoolName;
    private String batch;       // QUOTA / TONGZHAO
    private int priority;
    private Long highSchoolId;
    private String highSchoolName;
}
