package com.gqm2026.student.application;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.QuotaEligibilityService;
import com.gqm2026.student.service.StudentTieBreakComparator;
import com.gqm2026.student.simulator.ApplicationSimulator;
import com.gqm2026.student.simulator.ReferenceData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 志愿应用服务契约测试：提交锁 / 提交后增删改 MUST 返回 400 / 模拟返回 {generated:N}
 * （对应 04 §4.4、02 §2.5-3）。校验逻辑已内聚到 ApplicationAppService，测试从 controller 迁移至此（09 §6）。
 */
class ApplicationAppServiceTest {

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

    private static Student student(int score, long jsId, boolean eligible) {
        return Student.builder().name("t").ticketNo("T").juniorSchoolId(jsId)
                .chinese(0).math(0).english(0).physics(0).politics(0).pe(0)
                .totalScore(score).hasQuotaEligibility(eligible).build();
    }

    private ApplicationAppService appWith(SchoolReferencePort port, ApplicationRepository appRepo,
                                           StudentRepository studentRepo) {
        ApplicationSimulator simulator = new ApplicationSimulator(port, appRepo, studentRepo);
        QuotaEligibilityService qes = new QuotaEligibilityService(port, studentRepo, new StudentTieBreakComparator());
        return new ApplicationAppService(appRepo, studentRepo, simulator, qes);
    }

    @Test
    void submitLocksStudent() {
        StudentRepository studentRepo = mock(StudentRepository.class);
        Student s = student(480, 1L, true);
        s.setId(1L);
        s.setSubmitted(false);
        when(studentRepo.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        ApplicationAppService app = appWith(port, mock(ApplicationRepository.class), studentRepo);

        Student r = app.submit(1L);

        assertTrue(r.isSubmitted(), "submit 后志愿应锁定");
    }

    @Test
    void reopenUnlocksStudent() {
        StudentRepository studentRepo = mock(StudentRepository.class);
        Student s = student(480, 1L, true);
        s.setId(1L);
        s.setSubmitted(true);
        when(studentRepo.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        ApplicationAppService app = appWith(port, mock(ApplicationRepository.class), studentRepo);

        Student r = app.reopen(1L);

        assertFalse(r.isSubmitted(), "reopen 后应解除锁定");
    }

    @Test
    void createApplicationBlockedWhenSubmitted() {
        StudentRepository studentRepo = mock(StudentRepository.class);
        Student s = student(480, 1L, true);
        s.setId(1L);
        s.setSubmitted(true);
        when(studentRepo.findById(1L)).thenReturn(Optional.of(s));
        Application a = new Application();
        a.setStudentId(1L);
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        ApplicationAppService app = appWith(port, appRepo, studentRepo);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> app.createApplication(a));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(appRepo, never()).save(any());
    }

    @Test
    void createApplicationAllowedWhenEditable() {
        StudentRepository studentRepo = mock(StudentRepository.class);
        Student s = student(480, 1L, true);
        s.setId(1L);
        s.setSubmitted(false);
        when(studentRepo.findById(1L)).thenReturn(Optional.of(s));
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        ApplicationAppService app = appWith(port, appRepo, studentRepo);
        Application a = new Application();
        a.setStudentId(1L);

        Application r = app.createApplication(a);

        assertNotNull(r);
        verify(appRepo).save(a);
    }

    /** 模拟器契约：simulateApplications 返回 {generated:N}（generated = 实际落库志愿数） */
    @Test
    void simulateApplications_returnsGeneratedCount() {
        SchoolReferencePort port = mock(SchoolReferencePort.class);
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        when(port.fetch()).thenReturn(buildRef());
        Student s = student(480, 1L, true);
        s.setId(1L);
        when(studentRepo.findAll()).thenReturn(List.of(s));
        when(appRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(appRepo).deleteAll();
        doNothing().when(appRepo).flush();
        ApplicationAppService app = appWith(port, appRepo, studentRepo);

        Map<String, Object> r = app.simulateApplications();

        ArgumentCaptor<List<Application>> cap = ArgumentCaptor.forClass(List.class);
        verify(appRepo, atLeast(1)).saveAll(cap.capture());
        int saved = cap.getAllValues().stream().mapToInt(List::size).sum();
        assertEquals(saved, r.get("generated"));
        assertTrue(saved > 0, "未提交考生应生成志愿");
    }
}
