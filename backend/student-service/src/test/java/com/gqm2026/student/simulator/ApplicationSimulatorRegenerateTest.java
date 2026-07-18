package com.gqm2026.student.simulator;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * regenerateAll 提交锁契约：已 submitted 考生 MUST 被跳过、不被覆盖（对应 07 §7.3.3，阶段1 补 D6）。
 * 招生资源由 SchoolReferencePort（ACL 端口）提供，测试以接口 mock 替换原 SchoolDataFetcher（09 §6）。
 */
@ExtendWith(MockitoExtension.class)
class ApplicationSimulatorRegenerateTest {

    @Mock private SchoolReferencePort schoolReferencePort;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private StudentRepository studentRepository;

    @InjectMocks
    private ApplicationSimulator simulator;

    private static final Map<Integer, Integer> RANK_2026 = Map.of(
            480, 800, 465, 1300, 508, 10, 440, 3000, 420, 5000);
    private static final Map<Integer, Integer> RANK_2025 = Map.of(
            476, 700, 474, 900, 470, 1200, 465, 1500, 455, 1900,
            478, 600, 479, 550, 472, 1000, 471, 1100);

    private static ReferenceData buildRef() {
        ReferenceData.HighSchoolView h1 = new ReferenceData.HighSchoolView(1L, "KEY", 1, 120);
        ReferenceData.HighSchoolView h2 = new ReferenceData.HighSchoolView(2L, "KEY", 2, 110);
        ReferenceData.HighSchoolView h3 = new ReferenceData.HighSchoolView(3L, "NORMAL", 2, 100);
        ReferenceData.HighSchoolView h4 = new ReferenceData.HighSchoolView(4L, "KEY", 3, 90);
        ReferenceData.HighSchoolView h5 = new ReferenceData.HighSchoolView(5L, "NORMAL", 3, 80);
        List<ReferenceData.HighSchoolView> hs = List.of(h1, h2, h3, h4, h5);
        Map<Long, ReferenceData.HighSchoolView> byId = Map.of(1L, h1, 2L, h2, 3L, h3, 4L, h4, 5L, h5);
        Map<Long, Integer> jsZone = Map.of(1L, 1, 2L, 1, 3L, 2, 4L, 3);
        Map<Long, List<Long>> quota = Map.of(1L, List.of(1L, 2L), 2L, List.of(1L, 3L),
                3L, List.of(2L, 4L), 4L, List.of(5L, 3L));
        Map<Long, Integer> line2025 = Map.of(1L, 476, 2L, 474, 3L, 470, 4L, 465, 5L, 455);
        return new ReferenceData(hs, byId, jsZone, quota, 430, line2025, RANK_2025, RANK_2026);
    }

    private static Student stu(long id, boolean submitted) {
        return Student.builder().id(id).name("t").ticketNo("T").juniorSchoolId(1L)
                .chinese(0).math(0).english(0).physics(0).politics(0).pe(0)
                .totalScore(480).hasQuotaEligibility(true).submitted(submitted).build();
    }

    @Test
    void regenerateAll_skipsSubmittedStudents() {
        when(schoolReferencePort.fetch()).thenReturn(buildRef());
        Student s1 = stu(1L, false);
        Student s2 = stu(2L, true);   // 已提交 -> 必须跳过
        Student s3 = stu(3L, false);
        when(studentRepository.findAll()).thenReturn(List.of(s1, s2, s3));
        when(applicationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(applicationRepository).deleteAll();
        doNothing().when(applicationRepository).flush();

        int generated = simulator.regenerateAll();

        assertTrue(generated > 0, "未提交考生应生成志愿");
        ArgumentCaptor<List<Application>> cap = ArgumentCaptor.forClass(List.class);
        verify(applicationRepository, atLeast(1)).saveAll(cap.capture());
        List<Long> studentIds = cap.getAllValues().stream()
                .flatMap(List::stream).map(Application::getStudentId).collect(Collectors.toList());
        assertFalse(studentIds.contains(2L), "已提交考生(2)的志愿绝不能被生成/覆盖");
    }
}
