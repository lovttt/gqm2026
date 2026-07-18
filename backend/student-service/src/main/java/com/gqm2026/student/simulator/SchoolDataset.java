package com.gqm2026.student.simulator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 从 school-service {@code GET /school/export} 拉取的复合快照。
 * 字段与 school-service 的 SchoolDataset 序列化结果对齐（含 tier/zone）。
 */
public class SchoolDataset {

    public List<HighSchoolInfo> highSchools;
    public List<JuniorSchoolInfo> juniorSchools;
    public List<QuotaSeatInfo> quotaSeats;
    /** 共享校额名额的初中校分组（数据库 id 分组），用于志愿生成时合并对口高中 */
    public List<List<Long>> quotaGroups;
    public ControlLineInfo controlLine;
    public List<ScoreLineInfo> scoreLines;
    public List<ScoreSegmentInfo> scoreSegments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreSegmentInfo {
        public int year;
        public int score;
        public Integer cumulative;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighSchoolInfo {
        public Long id;
        public String name;
        public String district;
        public int tongzhaoQuota;
        public String tier;   // KEY / NORMAL
        public int zone;
        public String gaokaoTier;   // G7-Q2：TOP / HEAD / MID（nullable，旧快照可能缺）
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreLineInfo {
        public Long id;
        public Long highSchoolId;
        public int year;
        public String batch;
        public int score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JuniorSchoolInfo {
        public Long id;
        public String name;
        public String district;
        public int zone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaSeatInfo {
        public Long id;
        public Long juniorSchoolId;
        public Long highSchoolId;
        public int quota;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ControlLineInfo {
        public Long id;
        public String type;
        public int value;
    }
}
