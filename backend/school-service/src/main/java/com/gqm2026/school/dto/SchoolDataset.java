package com.gqm2026.school.dto;

import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.entity.ScoreLine;
import com.gqm2026.school.entity.ScoreSegment;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolDataset {
    private List<HighSchool> highSchools;
    private List<JuniorSchool> juniorSchools;
    private List<QuotaSeat> quotaSeats;
    /** 共享校额名额的初中校分组（数据库 id 分组），供模拟录取合并名额池使用 */
    private List<List<Long>> quotaGroups;
    private ControlLine controlLine;
    private List<ScoreLine> scoreLines;
    private List<ScoreSegment> scoreSegments;
}
