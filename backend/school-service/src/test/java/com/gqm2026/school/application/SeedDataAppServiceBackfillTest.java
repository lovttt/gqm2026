package com.gqm2026.school.application;

import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import com.gqm2026.school.repository.ScoreLineRepository;
import com.gqm2026.school.repository.ScoreSegmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/** 种子回填幂等契约（按 junior_school.csv 名称对齐回填班数/毕业生数，对应 02 §2.6，阶段1） */
@ExtendWith(MockitoExtension.class)
class SeedDataAppServiceBackfillTest {

    @Mock private HighSchoolRepository highSchoolRepository;
    @Mock private JuniorSchoolRepository juniorSchoolRepository;
    @Mock private QuotaSeatRepository quotaSeatRepository;
    @Mock private ControlLineRepository controlLineRepository;
    @Mock private ScoreLineRepository scoreLineRepository;
    @Mock private ScoreSegmentRepository scoreSegmentRepository;

    @InjectMocks
    private SeedDataAppService seedService;

    @Test
    void seedIfEmpty_whenAlreadySeeded_backfillsJuniorStatsFromCsv() throws Exception {
        // 指向测试资源目录（含 junior_school.csv；分段表 csv 不存在会被 catch 跳过）
        ReflectionTestUtils.setField(seedService, "seedDir", "src/test/resources/seed");
        when(highSchoolRepository.findAll()).thenReturn(List.of(new HighSchool())); // 非空 -> 走回填分支
        JuniorSchool js = JuniorSchool.builder().id(1L).name("测试一中").build();
        when(juniorSchoolRepository.findAll()).thenReturn(List.of(js));
        when(juniorSchoolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seedService.seedIfEmpty();

        verify(juniorSchoolRepository).save(argThat(s -> s.getName().equals("测试一中")
                && s.getClassCount() == 3 && s.getGradCount() == 120));
    }

    @Test
    void seedIfEmpty_whenAlreadySeeded_idempotentNoDuplicateInsert() throws Exception {
        ReflectionTestUtils.setField(seedService, "seedDir", "src/test/resources/seed");
        when(highSchoolRepository.findAll()).thenReturn(List.of(new HighSchool()));
        JuniorSchool js = JuniorSchool.builder().id(1L).name("测试一中").classCount(3).gradCount(120).build();
        when(juniorSchoolRepository.findAll()).thenReturn(List.of(js));

        seedService.seedIfEmpty();

        // 值已一致 -> backfill 不应触发 save
        verify(juniorSchoolRepository, never()).save(any());
    }
}
