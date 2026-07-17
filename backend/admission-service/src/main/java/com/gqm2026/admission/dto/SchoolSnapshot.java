package com.gqm2026.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 从 school-service /school/export 拉取的快照 */
public class SchoolSnapshot {

    public List<HighSchoolInfo> highSchools;
    public List<JuniorSchoolInfo> juniorSchools;
    public List<QuotaSeatInfo> quotaSeats;
    /** 共享校额名额的初中校分组（数据库 id 分组），用于校额到校合并名额池 */
    public List<List<Long>> quotaGroups;
    public ControlLineInfo controlLine;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighSchoolInfo {
        public Long id;
        public String name;
        public String district;
        public int tongzhaoQuota;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JuniorSchoolInfo {
        public Long id;
        public String name;
        public String district;
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
