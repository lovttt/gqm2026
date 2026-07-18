package com.gqm2026.school.application;

import com.gqm2026.school.domain.QuotaGroupService;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 校额名额分组合并契约测试（纯 Mock，对应 04 §4.6 / QuotaGroupConfig，阶段1） */
@ExtendWith(MockitoExtension.class)
class QuotaSeatAppServiceTest {

    @Mock
    private QuotaSeatRepository quotaSeatRepository;
    @Mock
    private JuniorSchoolRepository juniorSchoolRepository;

    private QuotaGroupService quotaGroupService;
    private QuotaSeatAppService appService;

    @BeforeEach
    void setup() {
        quotaGroupService = new QuotaGroupService(juniorSchoolRepository, quotaSeatRepository);
        appService = new QuotaSeatAppService(quotaSeatRepository, quotaGroupService);
    }

    private static JuniorSchool js(long id, String name) {
        return JuniorSchool.builder().id(id).name(name).build();
    }
    private static QuotaSeat qs(long id, long jsId, long hsId, int quota) {
        return QuotaSeat.builder().id(id).juniorSchoolId(jsId).highSchoolId(hsId).quota(quota).build();
    }

    @Test
    void juniorGroupQuery_mergesSeatsByHighSchool() {
        // 东直门(1) 与 第一六五中学(2) 同组
        when(juniorSchoolRepository.findById(1L)).thenReturn(Optional.of(js(1L, "北京市东直门中学")));
        when(juniorSchoolRepository.findByNameIn(List.of("北京市东直门中学", "北京市第一六五中学")))
                .thenReturn(List.of(js(1L, "北京市东直门中学"), js(2L, "北京市第一六五中学")));
        when(quotaSeatRepository.findByJuniorSchoolIdIn(List.of(1L, 2L))).thenReturn(List.of(
                qs(10, 1L, 1L, 5), qs(11, 2L, 1L, 3), qs(12, 1L, 2L, 4)));

        Page<QuotaSeat> page = appService.listQuotaSeats(1L, null, Pageable.unpaged());

        assertEquals(2, page.getContent().size());
        QuotaSeat hs1 = page.getContent().stream().filter(q -> q.getHighSchoolId() == 1L).findFirst().orElseThrow();
        QuotaSeat hs2 = page.getContent().stream().filter(q -> q.getHighSchoolId() == 2L).findFirst().orElseThrow();
        assertEquals(8, hs1.getQuota(), "组内两校在高中1的名额应合并求和 5+3");
        assertEquals(4, hs2.getQuota(), "高中2名额 4");
        assertEquals("北京市东直门中学 / 北京市第一六五中学", hs1.getJuniorSchoolNames());
        assertEquals("北京市东直门中学 / 北京市第一六五中学", hs2.getJuniorSchoolNames());
    }

    @Test
    void nonGroupJuniorQuery_returnsOwnSeatsWithoutMerge() {
        when(juniorSchoolRepository.findById(3L)).thenReturn(Optional.of(js(3L, "独立初中")));
        when(quotaSeatRepository.findByJuniorSchoolId(3L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(qs(20, 3L, 7L, 9))));

        Page<QuotaSeat> page = appService.listQuotaSeats(3L, null, Pageable.unpaged());

        assertEquals(1, page.getContent().size());
        assertEquals(9, page.getContent().get(0).getQuota());
        assertNull(page.getContent().get(0).getJuniorSchoolNames());
    }

    @Test
    void bothJuniorAndHighSchool_exactQueryNoMerge() {
        when(quotaSeatRepository.findByJuniorSchoolIdAndHighSchoolId(1L, 5L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(qs(30, 1L, 5L, 6))));

        Page<QuotaSeat> page = appService.listQuotaSeats(1L, 5L, Pageable.unpaged());

        assertEquals(1, page.getContent().size());
        assertEquals(5L, page.getContent().get(0).getHighSchoolId());
        assertEquals(6, page.getContent().get(0).getQuota());
    }
}
