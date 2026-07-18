package com.gqm2026.student.simulator;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationSimulatorTest {

    // 2026 一分一段：考生分数 -> 区排名
    private static final Map<Integer, Integer> RANK_2026 = Map.of(
            480, 800, 465, 1300, 508, 10, 440, 3000, 420, 5000);
    // 2025 一分一段：学校统招线 -> 区排名
    private static final Map<Integer, Integer> RANK_2025 = Map.of(
            476, 700, 474, 900, 470, 1200, 465, 1500, 455, 1900,
            478, 600, 479, 550, 472, 1000, 471, 1100);

    private static final ReferenceData REF = buildReferenceData();

    private final ApplicationSimulator simulator =
            new ApplicationSimulator(mock(SchoolReferencePort.class),
                    mock(ApplicationRepository.class), mock(StudentRepository.class));

    private static ReferenceData buildReferenceData() {
        ReferenceData.HighSchoolView h1 = new ReferenceData.HighSchoolView(1L, "KEY", 1, 120);
        ReferenceData.HighSchoolView h2 = new ReferenceData.HighSchoolView(2L, "KEY", 2, 110);
        ReferenceData.HighSchoolView h3 = new ReferenceData.HighSchoolView(3L, "NORMAL", 2, 100);
        ReferenceData.HighSchoolView h4 = new ReferenceData.HighSchoolView(4L, "KEY", 3, 90);
        ReferenceData.HighSchoolView h5 = new ReferenceData.HighSchoolView(5L, "NORMAL", 3, 80);
        List<ReferenceData.HighSchoolView> hs = List.of(h1, h2, h3, h4, h5);
        Map<Long, ReferenceData.HighSchoolView> byId = Map.of(
                1L, h1, 2L, h2, 3L, h3, 4L, h4, 5L, h5);
        Map<Long, Integer> jsZone = Map.of(1L, 1, 2L, 1, 3L, 2, 4L, 3);
        Map<Long, List<Long>> quota = Map.of(
                1L, List.of(1L, 2L),
                2L, List.of(1L, 3L),
                3L, List.of(2L, 4L),
                4L, List.of(5L, 3L));
        // 各校 2025 统招线
        Map<Long, Integer> line2025 = Map.of(1L, 476, 2L, 474, 3L, 470, 4L, 465, 5L, 455);
        return new ReferenceData(hs, byId, jsZone, quota, 430, line2025, RANK_2025, RANK_2026);
    }

    private static Student student(int score, long jsId, boolean eligible) {
        return Student.builder()
                .name("t").ticketNo("T").juniorSchoolId(jsId)
                .chinese(0).math(0).english(0).physics(0).politics(0).pe(0)
                .totalScore(score).hasQuotaEligibility(eligible).build();
    }

    private static List<Long> ids(List<Application> apps, String batch) {
        return apps.stream().filter(a -> batch.equals(a.getBatch()))
                .sorted(Comparator.comparingInt(Application::getPriority))
                .map(Application::getHighSchoolId).collect(Collectors.toList());
    }

    @Test
    void eligibleHighScorer_quotaThenTongzhao() {
        List<Application> apps = simulator.simulate(student(480, 1L, true), REF);
        // 校额：对口高中按 层次(均 KEY) + 离家近 排序 -> hs1(zone1) 先于 hs2(zone2)
        assertEquals(List.of(1L, 2L), ids(apps, "QUOTA"));
        // 480(排名800): h1线476->700(冲), h2线474->900(稳), h3线470->1200(稳),
        //               h4线465->1500(保), h5线455->1900(保)
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(apps, "TONGZHAO"));
    }

    @Test
    void nonEligible_noQuotaButTongzhao() {
        List<Application> apps = simulator.simulate(student(480, 1L, false), REF);
        assertTrue(ids(apps, "QUOTA").isEmpty(), "无校额资格者不得填报校额志愿");
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids(apps, "TONGZHAO"));
    }

    @Test
    void belowControlLine_noApplication() {
        List<Application> apps = simulator.simulate(student(420, 1L, true), REF);
        assertTrue(apps.isEmpty(), "低于控制线(430)者既无校额也无统招志愿");
    }

    @Test
    void chongWenBao_spreadByRank() {
        // 465(排名1300): h1线476->700(冲,diff-600), h2线474->900(冲,diff-400),
        //                h3线470->1200(冲,diff-100), h4线465->1500(稳,diff+200),
        //                h5线455->1900(保,diff+600)
        List<Application> apps = simulator.simulate(student(465, 1L, true), REF);
        List<Long> tz = ids(apps, "TONGZHAO");
        assertEquals(List.of(3L, 2L, 1L, 4L, 5L), tz);
        // 冲(3,2,1) 在前，稳(4) 居中，保(5) 在后
        assertEquals(5, tz.size());
        // 普通校被纳入（h3、h5），并非全是重点校
        assertTrue(tz.contains(3L) && tz.contains(5L));
    }

    @Test
    void tongzhaoNeverExceedsEight() {
        List<Application> apps = simulator.simulate(student(480, 1L, true), REF);
        assertEquals(5, ids(apps, "TONGZHAO").size());
        // 构造 9 所高中场景，验证上限 8（冲3/稳3/保2）
        ReferenceData.HighSchoolView e6 = new ReferenceData.HighSchoolView(6L, "KEY", 1, 70);
        ReferenceData.HighSchoolView e7 = new ReferenceData.HighSchoolView(7L, "KEY", 1, 70);
        ReferenceData.HighSchoolView e8 = new ReferenceData.HighSchoolView(8L, "NORMAL", 1, 60);
        ReferenceData.HighSchoolView e9 = new ReferenceData.HighSchoolView(9L, "NORMAL", 1, 60);
        List<ReferenceData.HighSchoolView> bigHs = new ArrayList<>(REF.highSchools);
        bigHs.addAll(List.of(e6, e7, e8, e9));
        Map<Long, ReferenceData.HighSchoolView> bigById = new HashMap<>(REF.hsById);
        bigById.put(6L, e6); bigById.put(7L, e7); bigById.put(8L, e8); bigById.put(9L, e9);
        Map<Long, Integer> bigLine = new HashMap<>(REF.line2025ById);
        bigLine.put(6L, 478); bigLine.put(7L, 479); bigLine.put(8L, 472); bigLine.put(9L, 471);
        ReferenceData big = new ReferenceData(bigHs, bigById, REF.juniorZoneById,
                REF.quotaHighSchoolsByJs, 430, bigLine, RANK_2025, RANK_2026);
        List<Application> bigApps = simulator.simulate(student(480, 1L, true), big);
        assertEquals(8, ids(bigApps, "TONGZHAO").size());
    }
}
