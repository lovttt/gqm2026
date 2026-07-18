package com.gqm2026.student.service;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.simulator.SchoolDataset;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 校验「校额到校资格结合初中校名额总数」（对应 02 §2.6 / 04 §4.4）。
 * 招生资源由 SchoolReferencePort（ACL 端口）提供，测试以接口 mock 替换原 SchoolDataFetcher（09 §6）。
 */
class QuotaEligibilityServiceTest {

    private Student stu(long id, long jsId, int total, String ticket) {
        Student s = Student.builder()
                .name("t").ticketNo(ticket).juniorSchoolId(jsId)
                .chinese(0).math(0).english(0).physics(0).politics(0).pe(0)
                .totalScore(total).hasQuotaEligibility(false).build();
        s.setId(id);
        return s;
    }

    private SchoolDataset schoolData(int controlLine, Map<Long, Integer> quotaTotalByJs) {
        SchoolDataset sd = new SchoolDataset();
        sd.controlLine = new SchoolDataset.ControlLineInfo(1L, "QUOTA", controlLine);
        sd.quotaSeats = new ArrayList<>();
        long qid = 1;
        for (Map.Entry<Long, Integer> e : quotaTotalByJs.entrySet()) {
            // 用单条名额表达该初中校名额总数（金额聚合逻辑在服务里）
            sd.quotaSeats.add(new SchoolDataset.QuotaSeatInfo(qid++, e.getKey(), 99L, e.getValue()));
        }
        return sd;
    }

    private QuotaEligibilityService build(List<Student> students, SchoolDataset sd) {
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        when(port.fetchRaw()).thenReturn(sd);
        StudentRepository repo = mock(StudentRepository.class);
        when(repo.findAll()).thenReturn(students);
        when(repo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        return new QuotaEligibilityService(port, repo, new StudentTieBreakComparator());
    }

    @Test
    void topNEligible_restExcluded() {
        // 初中校1 名额总数 N=2；达线考生 3 人(460/450/440) + 1 人低于线(420)
        Student s1 = stu(1, 1L, 460, "T001");
        Student s2 = stu(2, 1L, 450, "T002");
        Student s3 = stu(3, 1L, 440, "T003");
        Student s4 = stu(4, 1L, 420, "T004"); // 低于控制线
        List<Student> all = new ArrayList<>(List.of(s1, s2, s3, s4));
        QuotaEligibilityService svc = build(all, schoolData(430, Map.of(1L, 2)));

        Map<String, Object> r = svc.recompute();

        assertTrue(s1.isHasQuotaEligibility(), "校内第1名应具资格");
        assertTrue(s2.isHasQuotaEligibility(), "校内第2名应具资格");
        assertFalse(s3.isHasQuotaEligibility(), "超出前 N 名(第3名)应无资格");
        assertFalse(s4.isHasQuotaEligibility(), "低于控制线应无资格");
        assertEquals(2, r.get("eligibleTotal"));
        assertEquals(4, r.get("studentTotal"));
    }

    @Test
    void passingFewerThanN_allEligible() {
        // 初中校2 名额总数 N=5；但达线仅 3 人 → 全部具资格（不越界）
        Student s5 = stu(5, 2L, 445, "T005");
        Student s6 = stu(6, 2L, 435, "T006");
        Student s7 = stu(7, 2L, 431, "T007");
        Student s8 = stu(8, 2L, 400, "T008"); // 低于线
        List<Student> all = new ArrayList<>(List.of(s5, s6, s7, s8));
        QuotaEligibilityService svc = build(all, schoolData(430, Map.of(2L, 5)));

        Map<String, Object> r = svc.recompute();

        assertTrue(s5.isHasQuotaEligibility());
        assertTrue(s6.isHasQuotaEligibility());
        assertTrue(s7.isHasQuotaEligibility());
        assertFalse(s8.isHasQuotaEligibility());
        assertEquals(3, r.get("eligibleTotal"), "资格数 = min(N, 达线人数) = 3");
    }

    @Test
    void zeroQuota_noneEligible() {
        // 初中校3 未分配名额(N=0) → 该校无人具资格，即使高分
        Student s9 = stu(9, 3L, 500, "T009");
        Student s10 = stu(10, 3L, 470, "T010");
        List<Student> all = new ArrayList<>(List.of(s9, s10));
        QuotaEligibilityService svc = build(all, schoolData(430, Map.of())); // 无任何名额

        Map<String, Object> r = svc.recompute();

        assertFalse(s9.isHasQuotaEligibility());
        assertFalse(s10.isHasQuotaEligibility());
        assertEquals(0, r.get("eligibleTotal"));
    }

    @Test
    void previouslyEligible_resetWhenOverCapacity() {
        // 之前误标为具资格者，重算后若超出前 N 名应被重置为无资格
        Student s1 = stu(1, 1L, 460, "T001");
        Student s2 = stu(2, 1L, 450, "T002");
        Student s3 = stu(3, 1L, 440, "T003");
        s3.setHasQuotaEligibility(true); // 旧值：误标
        List<Student> all = new ArrayList<>(List.of(s1, s2, s3));
        QuotaEligibilityService svc = build(all, schoolData(430, Map.of(1L, 2)));

        svc.recompute();

        assertFalse(s3.isHasQuotaEligibility(), "重算须清掉超额者的旧资格");
    }
}
