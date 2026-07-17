package com.gqm2026.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 从 student-service /student/export 拉取的快照 */
public class StudentSnapshot {

    public List<StudentInfo> students;
    public List<ApplicationInfo> applications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        public Long id;
        public String name;
        public String ticketNo;
        public Long juniorSchoolId;
        public int chinese;
        public int math;
        public int english;
        public int physics;
        public int politics;
        public int pe;
        public int totalScore;
        public boolean hasQuotaEligibility;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        public Long id;
        public Long studentId;
        public String batch;   // QUOTA / TONGZHAO
        public int priority;
        public Long highSchoolId;
    }
}
