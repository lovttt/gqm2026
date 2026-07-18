package com.gqm2026.student.controller;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.service.QuotaEligibilityService;
import com.gqm2026.student.simulator.ApplicationSimulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 志愿提交锁契约测试：提交后增删改志愿 MUST 返回 400（对应 04 §4.4、02 §2.5-3） */
@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationSimulator simulator;

    @Mock
    private QuotaEligibilityService quotaEligibilityService;

    @InjectMocks
    private ApplicationController controller;

    @Test
    void submitLocksStudent() {
        Student s = new Student();
        s.setId(1L);
        s.setSubmitted(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Student r = controller.submit(1L);

        assertTrue(r.isSubmitted(), "submit 后志愿应锁定");
    }

    @Test
    void reopenUnlocksStudent() {
        Student s = new Student();
        s.setId(1L);
        s.setSubmitted(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Student r = controller.reopen(1L);

        assertFalse(r.isSubmitted(), "reopen 后应解除锁定");
    }

    @Test
    void createApplicationBlockedWhenSubmitted() {
        Student s = new Student();
        s.setId(1L);
        s.setSubmitted(true);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        Application a = new Application();
        a.setStudentId(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.createApplication(a));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplicationAllowedWhenEditable() {
        Student s = new Student();
        s.setId(1L);
        s.setSubmitted(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(s));
        when(applicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Application a = new Application();
        a.setStudentId(1L);

        Application r = controller.createApplication(a);

        assertNotNull(r);
    }

    /** 模拟器控制器契约：POST /applications/simulate 返回 {generated:N}（对应 07 §7.4，阶段1 补 D6） */
    @Test
    void simulateApplications_returnsGeneratedCount() {
        when(simulator.regenerateAll()).thenReturn(7);

        Map<String, Object> r = controller.simulateApplications();

        assertEquals(7, r.get("generated"));
        verify(simulator).regenerateAll();
    }
}
