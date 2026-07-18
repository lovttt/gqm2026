package com.gqm2026.admission.engine;

import com.gqm2026.admission.dto.SchoolSnapshot;
import com.gqm2026.admission.dto.StudentSnapshot;
import com.gqm2026.admission.entity.AdmissionResult;
import com.gqm2026.admission.repository.AdmissionResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 录取领域服务契约测试。重构后 {@link AdmissionEngine} 不再直连 HTTP，
 * 由测试方法直接注入 school/student 快照（对应 09 §3.2 端口化）。
 */
@ExtendWith(MockitoExtension.class)
class AdmissionEngineTest {

    @Mock
    private AdmissionResultRepository resultRepo;
    @Captor
    private ArgumentCaptor<AdmissionResult> captor;

    private final TieBreakComparator tieBreak = new TieBreakComparator();
    private AdmissionEngine engine;

    @BeforeEach
    void setup() {
        engine = new AdmissionEngine(resultRepo, tieBreak);
        // 每次调用返回递增的 runId，模拟多次运行互不覆盖
        AtomicLong counter = new AtomicLong(0);
        when(resultRepo.findLatestRunId()).thenAnswer(inv -> counter.incrementAndGet());
        when(resultRepo.save(any(AdmissionResult.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SchoolSnapshot school(List<SchoolSnapshot.HighSchoolInfo> hs,
                                  List<SchoolSnapshot.QuotaSeatInfo> quota,
                                  int controlLine) {
        SchoolSnapshot ss = new SchoolSnapshot();
        ss.highSchools = hs;
        ss.juniorSchools = List.of(new SchoolSnapshot.JuniorSchoolInfo(1L, "J1", "东城"));
        ss.quotaSeats = quota;
        ss.controlLine = new SchoolSnapshot.ControlLineInfo(1L, "QUOTA", controlLine);
        return ss;
    }

    private SchoolSnapshot.HighSchoolInfo hs(long id, int tongzhaoQuota) {
        return new SchoolSnapshot.HighSchoolInfo(id, "HS" + id, "东城", tongzhaoQuota);
    }

    private SchoolSnapshot.QuotaSeatInfo qs(long junior, long high, int quota) {
        return new SchoolSnapshot.QuotaSeatInfo(1L, junior, high, quota);
    }

    private StudentSnapshot.StudentInfo stu(long id, String ticket, long junior, int total, boolean quota) {
        return new StudentSnapshot.StudentInfo(id, "S" + id, ticket, junior,
                90, 90, 90, 80, 80, 50, total, quota);
    }

    private StudentSnapshot.ApplicationInfo app(long id, long sid, String batch, int prio, long hs) {
        return new StudentSnapshot.ApplicationInfo(id, sid, batch, prio, hs);
    }

    @Test
    void quotaControlLineAndTieBreakDeterminesAdmission() {
        // 1 个初中校、1 个高中、校额名额 1、控制线 430
        SchoolSnapshot ss = school(List.of(hs(10L, 5)), List.of(qs(1L, 10L, 1)), 430);
        // 两名考生同分 480、均过线且具备资格、都填报该校额志愿
        StudentSnapshot.StudentInfo s1 = stu(1L, "1001", 1L, 480, true);
        StudentSnapshot.StudentInfo s2 = stu(2L, "1002", 1L, 480, true);
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(s1, s2);
        st.applications = List.of(
                app(1L, 1L, "QUOTA", 1, 10L),
                app(2L, 2L, "QUOTA", 1, 10L),
                app(3L, 1L, "TONGZHAO", 1, 10L),
                app(4L, 2L, "TONGZHAO", 1, 10L));

        engine.runFull(ss, st);

        verify(resultRepo, atLeastOnce()).save(captor.capture());
        List<AdmissionResult> saved = captor.getAllValues();
        // 校额仅 1 个名额 -> 准考证号靠前的 s1 录取
        AdmissionResult quota = saved.stream()
                .filter(r -> "QUOTA".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(1L, quota.getStudentId());
        // s2 未中校额，但统招仍有余额 -> 统招录取
        AdmissionResult tongzhao = saved.stream()
                .filter(r -> "TONGZHAO".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(2L, tongzhao.getStudentId());
    }

    @Test
    void tongzhaoParallelVolunteerDoesNotPenalizeLowerPriorityChoice() {
        // 平行志愿：分数优先、遵循志愿、一次投档；填在 2 志愿不被罚到队尾
        SchoolSnapshot ss = school(List.of(hs(10L, 1), hs(20L, 1)), List.of(), 0);
        // 三人均无校额资格，隔离统招
        StudentSnapshot.StudentInfo x = stu(1L, "1001", 1L, 480, false); // [10#1]
        StudentSnapshot.StudentInfo y = stu(2L, "1002", 1L, 470, false); // [10#1, 20#2]
        StudentSnapshot.StudentInfo z = stu(3L, "1003", 1L, 460, false); // [20#1]
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(x, y, z);
        st.applications = List.of(
                app(1L, 1L, "TONGZHAO", 1, 10L),
                app(2L, 2L, "TONGZHAO", 1, 10L),
                app(3L, 2L, "TONGZHAO", 2, 20L),
                app(4L, 3L, "TONGZHAO", 1, 20L));

        engine.runFull(ss, st);

        verify(resultRepo, atLeastOnce()).save(captor.capture());
        Map<Long, AdmissionResult> byStudent = captor.getAllValues().stream()
                .collect(Collectors.toMap(AdmissionResult::getStudentId, r -> r));
        // X 高分占 10；Y 次高，10 已满转投 20（2 志愿）仍录取——无罚分
        assertEquals("ADMITTED", byStudent.get(1L).getStatus());
        assertEquals(10L, byStudent.get(1L).getHighSchoolId());
        assertEquals("ADMITTED", byStudent.get(2L).getStatus());
        assertEquals(20L, byStudent.get(2L).getHighSchoolId());
        // Z 分数最低，20 被 Y 占满 -> 滑档 NOT_ADMITTED
        assertEquals("NOT_ADMITTED", byStudent.get(3L).getStatus());
    }

    @Test
    void repeatedRunsAreDeterministic() {
        SchoolSnapshot ss = school(List.of(hs(10L, 5)), List.of(qs(1L, 10L, 1)), 430);
        StudentSnapshot.StudentInfo s1 = stu(1L, "1001", 1L, 480, true);
        StudentSnapshot.StudentInfo s2 = stu(2L, "1002", 1L, 480, true);
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(s1, s2);
        st.applications = List.of(
                app(1L, 1L, "QUOTA", 1, 10L),
                app(2L, 2L, "QUOTA", 1, 10L),
                app(3L, 1L, "TONGZHAO", 1, 10L),
                app(4L, 2L, "TONGZHAO", 1, 10L));

        engine.runFull(ss, st);
        engine.runFull(ss, st);

        verify(resultRepo, atLeast(2)).save(captor.capture());
        List<AdmissionResult> all = captor.getAllValues();
        Map<Long, List<AdmissionResult>> byRun = all.stream()
                .collect(Collectors.groupingBy(AdmissionResult::getRunId));
        assertEquals(2, byRun.size());
        List<Long> runIds = new ArrayList<>(byRun.keySet());
        Set<String> r1 = byRun.get(runIds.get(0)).stream()
                .map(r -> r.getStudentId() + "|" + r.getBatch() + "|" + r.getStatus())
                .collect(Collectors.toSet());
        Set<String> r2 = byRun.get(runIds.get(1)).stream()
                .map(r -> r.getStudentId() + "|" + r.getBatch() + "|" + r.getStatus())
                .collect(Collectors.toSet());
        assertEquals(r1, r2, "两次模拟结果应完全一致（确定性）");
    }

    @Test
    void quotaControlLineBoundary() {
        // 控制线 430：恰好达到应校额录取，差 1 分（429）不得校额录取（3.2 / 3.5-4）
        SchoolSnapshot ss = school(List.of(hs(10L, 5)), List.of(qs(1L, 10L, 1)), 430);
        StudentSnapshot.StudentInfo atLine = stu(1L, "1001", 1L, 430, true);
        StudentSnapshot.StudentInfo below  = stu(2L, "1002", 1L, 429, true);
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(atLine, below);
        st.applications = List.of(
                app(1L, 1L, "QUOTA", 1, 10L),
                app(2L, 2L, "QUOTA", 1, 10L),
                app(3L, 1L, "TONGZHAO", 1, 10L),
                app(4L, 2L, "TONGZHAO", 1, 10L));

        engine.runFull(ss, st);

        verify(resultRepo, atLeastOnce()).save(captor.capture());
        Set<Long> quotaAdmitted = captor.getAllValues().stream()
                .filter(r -> "QUOTA".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus()))
                .map(AdmissionResult::getStudentId)
                .collect(Collectors.toSet());
        assertTrue(quotaAdmitted.contains(1L), "总分恰好 430 应校额录取");
        assertFalse(quotaAdmitted.contains(2L), "总分 429 未满控制线，绝不得校额录取");
    }

    @Test
    void tongzhaoNotAdmittedWhenSeatsExhausted() {
        // 计划仅 1 个且被高分占满 -> 低分考生滑档 NOT_ADMITTED，且无补录/二次投档（3.3 / 3.7-1）
        SchoolSnapshot ss = school(List.of(hs(10L, 1)), List.of(), 0);
        StudentSnapshot.StudentInfo hi = stu(1L, "1001", 1L, 480, false);
        StudentSnapshot.StudentInfo lo = stu(2L, "1002", 1L, 460, false);
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(hi, lo);
        st.applications = List.of(
                app(1L, 1L, "TONGZHAO", 1, 10L),
                app(2L, 2L, "TONGZHAO", 1, 10L));

        engine.runFull(ss, st);

        verify(resultRepo, atLeastOnce()).save(captor.capture());
        Map<Long, List<AdmissionResult>> byStudent = captor.getAllValues().stream()
                .collect(Collectors.groupingBy(AdmissionResult::getStudentId));
        assertEquals("ADMITTED", byStudent.get(1L).get(0).getStatus());
        AdmissionResult loRes = byStudent.get(2L).get(0);
        assertEquals("NOT_ADMITTED", loRes.getStatus());
        assertEquals(1, byStudent.get(2L).size(), "滑档后不得进入补录或二次投档");
        assertNull(loRes.getHighSchoolId(), "滑档不落入任何高中");
    }

    @Test
    void quotaAdmittedExcludedFromTongzhao() {
        // 校额已录取者 MUST 从统招候选池剔除，不得重复录取（3.1 / 3.5-1）
        SchoolSnapshot ss = school(List.of(hs(10L, 5)), List.of(qs(1L, 10L, 1)), 430);
        StudentSnapshot.StudentInfo s = stu(1L, "1001", 1L, 480, true);
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of(s);
        st.applications = List.of(
                app(1L, 1L, "QUOTA", 1, 10L),
                app(2L, 1L, "TONGZHAO", 1, 10L));

        engine.runFull(ss, st);

        verify(resultRepo, atLeastOnce()).save(captor.capture());
        Set<String> batches = captor.getAllValues().stream()
                .map(AdmissionResult::getBatch)
                .collect(Collectors.toSet());
        assertTrue(batches.contains("QUOTA"));
        assertFalse(batches.contains("TONGZHAO"), "校额已录取者不得再出现在统招批次");
    }
}
