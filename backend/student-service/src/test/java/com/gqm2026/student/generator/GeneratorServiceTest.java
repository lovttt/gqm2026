package com.gqm2026.student.generator;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.generator.dto.*;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.simulator.ReferenceData;
import com.gqm2026.student.simulator.SchoolDataFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GeneratorServiceTest {

    private SchoolDataFetcher fetcher;
    private StudentRepository repo;
    private GeneratorService service;
    private static ReferenceData REF;

    @BeforeEach
    void setUp() {
        fetcher = Mockito.mock(SchoolDataFetcher.class);
        repo = Mockito.mock(StudentRepository.class);
        service = new GeneratorService(repo, fetcher,
                new ZoneCommuteEstimator(), new GaokaoTierResolver(), new StudentAttributes());
        REF = buildReferenceData();
        when(fetcher.fetch()).thenReturn(REF);
    }

    private static ReferenceData buildReferenceData() {
        ReferenceData.HighSchoolView h1 = new ReferenceData.HighSchoolView(1L, "KEY", 1, 120);
        ReferenceData.HighSchoolView h2 = new ReferenceData.HighSchoolView(2L, "KEY", 2, 110);
        ReferenceData.HighSchoolView h3 = new ReferenceData.HighSchoolView(3L, "NORMAL", 2, 100);
        ReferenceData.HighSchoolView h4 = new ReferenceData.HighSchoolView(4L, "KEY", 3, 90);
        ReferenceData.HighSchoolView h5 = new ReferenceData.HighSchoolView(5L, "NORMAL", 3, 80);
        List<ReferenceData.HighSchoolView> hs = List.of(h1, h2, h3, h4, h5);
        Map<Long, ReferenceData.HighSchoolView> byId = Map.of(1L, h1, 2L, h2, 3L, h3, 4L, h4, 5L, h5);
        Map<Long, Integer> jsZone = Map.of(1L, 1, 2L, 1, 3L, 2, 4L, 3);
        Map<Long, List<Long>> quota = Map.of(1L, List.of(1L, 2L));
        Map<Long, Integer> line2025 = Map.of(1L, 476, 2L, 474, 3L, 470, 4L, 465, 5L, 455);
        Map<Integer, Integer> rank2025 = Map.of(476, 700, 474, 900, 470, 1200, 465, 1500, 455, 1900);
        Map<Integer, Integer> rank2026 = Map.of(480, 800);
        return new ReferenceData(hs, byId, jsZone, quota, 430, line2025, rank2025, rank2026);
    }

    private Student student(int score, long jsId, boolean eligible) {
        return Student.builder().id(99L).name("考生X").ticketNo("T")
                .juniorSchoolId(jsId).chinese(0).math(0).english(0).physics(0).politics(0).pe(0)
                .totalScore(score).hasQuotaEligibility(eligible).build();
    }

    private GenerateRequest.GenerateRequestBuilder baseReq() {
        return GenerateRequest.builder().studentId(99L)
                .sprintWeight(30).steadyWeight(40).safetyWeight(30)
                .commuteWeight(50).gaokaoOutputWeight(50);
    }

    @Test
    void quotaThreshold_blockedWhenBelowControlLine() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(420, 1L, true)));
        GenerateResponse r = service.generate(baseReq().build());
        assertTrue(r.isQuotaEligible() == false || r.getQuotaPlan().isEmpty());
        assertTrue(r.getQuotaPlan().isEmpty(), "低于控制线的考生不得生成校额志愿");
        assertTrue(r.getIssues().stream().anyMatch(i -> "QUOTA_THRESHOLD".equals(i.getCode()) && "BLOCK".equals(i.getLevel())));
    }

    @Test
    void quotaThreshold_blockedWhenEvalBelowB() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().comprehensiveEval("C").build());
        assertTrue(r.getQuotaPlan().isEmpty());
        assertTrue(r.getIssues().stream().anyMatch(i -> "QUOTA_THRESHOLD".equals(i.getCode())));
    }

    @Test
    void guantongHiddenWhenBelow380() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(370, 1L, true)));
        GenerateResponse r = service.generate(baseReq().build());
        assertTrue(r.isGuantongHidden());
        assertTrue(r.getGuantongPlan().isEmpty());
        assertTrue(r.getIssues().stream().anyMatch(i -> "GUANTONG_THRESHOLD".equals(i.getCode())));
    }

    @Test
    void weightSumMustBe100() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().sprintWeight(30).steadyWeight(30).safetyWeight(30).build());
        assertTrue(r.getIssues().stream().anyMatch(i -> "WEIGHT_SUM".equals(i.getCode()) && "BLOCK".equals(i.getLevel())));
    }

    @Test
    void commuteCapFiltersFarSchools() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().commuteCapMinutes(20).build());
        // 仅 h1(同片区 commute15) 通过，其余因跨片区被过滤
        assertEquals(List.of(2L, 3L, 4L, 5L), r.getFilteredByCommute());
        assertEquals(1, r.getTongzhaoPlan().size());
        assertEquals(1L, r.getTongzhaoPlan().get(0).getHighSchoolId());
    }

    @Test
    void gaokaoTierPrefFiltersNonMatching() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().gaokaoTierPref("TOP").build());
        // NORMAL(h3,h5) 被过滤，KEY(h1,h2,h4) 保留
        assertEquals(List.of(3L, 5L), r.getFilteredByGaokaoTier());
    }

    @Test
    void gradientIntervalBandsAssigned() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().build());
        // 480 分下：h1(-4)稳、h2(-6)稳、h3(-10)保、h4(-15)保、h5(-25)区间外
        Map<Long, String> bandById = r.getTongzhaoPlan().stream()
                .collect(java.util.stream.Collectors.toMap(GeneratedChoice::getHighSchoolId, GeneratedChoice::getScoreBand));
        assertEquals("STEADY", bandById.get(1L));
        assertEquals("STEADY", bandById.get(2L));
        assertEquals("SAFETY", bandById.get(3L));
        assertEquals("SAFETY", bandById.get(4L));
    }

    @Test
    void capacityTruncationWarn() {
        // 构造 15 所校额对口校场景，验证 QUOTA 容量上限 8 与截断提示
        ReferenceData.HighSchoolView[] many = new ReferenceData.HighSchoolView[15];
        List<ReferenceData.HighSchoolView> hs = new ArrayList<>(REF.highSchools);
        Map<Long, ReferenceData.HighSchoolView> byId = new HashMap<>(REF.hsById);
        Map<Long, List<Long>> quota = new HashMap<>(REF.quotaHighSchoolsByJs);
        List<Long> quotaList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            long id = 100L + i;
            ReferenceData.HighSchoolView h = new ReferenceData.HighSchoolView(id, "KEY", 1, 100);
            many[i] = h; hs.add(h); byId.put(id, h); quotaList.add(id);
        }
        quota.put(1L, quotaList);
        ReferenceData big = new ReferenceData(hs, byId, REF.juniorZoneById, quota,
                REF.controlLine, REF.line2025ById, REF.rank2025ByScore, REF.rank2026ByScore);
        when(fetcher.fetch()).thenReturn(big);
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateResponse r = service.generate(baseReq().build());
        assertEquals(GeneratorConstants.QUOTA_CAP, r.getQuotaPlan().size());
        assertTrue(r.getIssues().stream().anyMatch(i -> "CAPACITY".equals(i.getCode()) && "QUOTA".equals(i.getBatch())));
    }

    @Test
    void preferenceWeightLinkageComparison() {
        when(repo.findById(99L)).thenReturn(Optional.of(student(480, 1L, true)));
        GenerateRequest req = baseReq()
                .commuteWeight(0).gaokaoOutputWeight(100)
                .prevCommuteWeight(100).prevGaokaoOutputWeight(0)
                .build();
        GenerateResponse r = service.generate(req);
        assertNotNull(r.getComparisons());
        PlanComparison tz = r.getComparisons().stream()
                .filter(c -> "TONGZHAO".equals(c.getBatch())).findFirst().orElse(null);
        assertNotNull(tz);
        assertNotNull(tz.getBefore());
        assertNotNull(tz.getAfter());
        // 偏好从「通勤优先」切到「高考出口优先」，排序应发生变化
        assertNotEquals(tz.getBefore(), tz.getAfter());
    }
}
