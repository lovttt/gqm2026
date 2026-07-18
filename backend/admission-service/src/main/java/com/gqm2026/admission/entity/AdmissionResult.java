package com.gqm2026.admission.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admission_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private String studentName;
    private String ticketNo;

    /** QUOTA=校额到校，TONGZHAO=统招 */
    private String batch;

    /** 录取的高中 id（未录取为 null） */
    private Long highSchoolId;
    private String highSchoolName;

    /** 考生来源初中校（录取结果反规范化，便于前端直接展示，无需回查 student-service） */
    private Long juniorSchoolId;
    private String juniorSchoolName;

    private int totalScore;

    /** 各科得分（反规范化自 student-service 快照，便于录取结果页直接展示，无需回查） */
    private int chinese;
    private int math;
    private int english;
    private int physics;
    private int politics;
    private int pe;

    /** 校额到校的校内排名：同初中校竞争同一高中校额时的名次；仅 QUOTA 且 ADMITTED 有值，其余为 null */
    private Integer schoolRank;

    /** ADMITTED / NOT_ADMITTED */
    private String status;

    private String note;

    /** 充血行为：是否已被录取（G7-Q7 录取状态判定） */
    public boolean isAdmitted() {
        return "ADMITTED".equals(status);
    }

    private LocalDateTime createdAt;

    /** 模拟运行批次号（同一 runFull/runQuota/runTongzhao 共享），用于保留多次模拟历史 */
    private Long runId;

    /** 本次模拟运行时间 */
    private LocalDateTime runAt;
}
