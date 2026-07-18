package com.gqm2026.admission.application;

import com.gqm2026.admission.dto.SchoolSnapshot;
import com.gqm2026.admission.dto.StudentSnapshot;
import com.gqm2026.admission.engine.AdmissionEngine;
import com.gqm2026.admission.engine.TieBreakComparator;
import com.gqm2026.admission.entity.AdmissionResult;
import com.gqm2026.admission.infrastructure.acl.SchoolSnapshotPort;
import com.gqm2026.admission.infrastructure.acl.StudentSnapshotPort;
import com.gqm2026.admission.repository.AdmissionResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 录取应用服务契约测试：编排用例（HTTP 拉快照在 TX 外、引擎计算在 TX 内）与查询委派。
 * 校验逻辑内聚到 AdmissionAppService + AdmissionEngine，测试从 controller 迁移至此（09 §6）。
 */
@ExtendWith(MockitoExtension.class)
class AdmissionAppServiceTest {

    @Mock
    private SchoolSnapshotPort schoolSnapshotPort;
    @Mock
    private StudentSnapshotPort studentSnapshotPort;
    @Mock
    private AdmissionResultRepository resultRepo;

    private AdmissionAppService app;

    @BeforeEach
    void setup() {
        AdmissionEngine engine = new AdmissionEngine(resultRepo, new TieBreakComparator());
        app = new AdmissionAppService(schoolSnapshotPort, studentSnapshotPort, engine);
    }

    private SchoolSnapshot schoolSnapshot() {
        SchoolSnapshot ss = new SchoolSnapshot();
        ss.highSchools = List.of(new SchoolSnapshot.HighSchoolInfo(10L, "HS", "东城", 5));
        ss.juniorSchools = List.of(new SchoolSnapshot.JuniorSchoolInfo(1L, "J", "东城"));
        ss.quotaSeats = List.of(new SchoolSnapshot.QuotaSeatInfo(1L, 1L, 10L, 1));
        ss.controlLine = new SchoolSnapshot.ControlLineInfo(1L, "QUOTA", 430);
        return ss;
    }

    @Test
    void runFullFetchesSnapshotsThenDelegatesToEngine() {
        SchoolSnapshot ss = schoolSnapshot();
        StudentSnapshot st = new StudentSnapshot();
        st.students = List.of();
        st.applications = List.of();
        when(schoolSnapshotPort.fetch()).thenReturn(ss);
        when(studentSnapshotPort.fetch()).thenReturn(st);
        when(resultRepo.findLatestRunId()).thenReturn(0L);

        Map<String, Object> r = app.runFull();

        assertEquals(1L, r.get("runId"), "runId 应在快照拉取后由引擎派生");
        verify(schoolSnapshotPort).fetch();
        verify(studentSnapshotPort).fetch();
    }

    @Test
    void runsGroupsResultsByRunId() {
        AdmissionResult a = AdmissionResult.builder().studentId(1L).batch("QUOTA").status("ADMITTED").runId(1L).build();
        AdmissionResult b = AdmissionResult.builder().studentId(2L).batch("TONGZHAO").status("ADMITTED").runId(1L).build();
        when(resultRepo.findAll()).thenReturn(List.of(a, b));

        List<Map<String, Object>> runs = app.runs();

        assertEquals(1, runs.size());
        assertEquals(1L, runs.get(0).get("runId"));
        assertEquals(2L, runs.get(0).get("admitted"));
    }

    @Test
    void summaryBySchoolDelegatesToEngineWithSnapshot() {
        SchoolSnapshot ss = schoolSnapshot();
        AdmissionResult admitted = AdmissionResult.builder()
                .studentId(1L).highSchoolId(10L).batch("TONGZHAO").status("ADMITTED").runId(1L).build();
        when(schoolSnapshotPort.fetch()).thenReturn(ss);
        when(resultRepo.findLatestRunId()).thenReturn(1L);
        when(resultRepo.findByRunId(1L)).thenReturn(List.of(admitted));

        List<Map<String, Object>> s = app.summaryBySchool();

        assertNotNull(s);
        assertEquals(1, s.size(), "应含快照中那所高中");
        assertEquals(10L, s.get(0).get("highSchoolId"));
        verify(schoolSnapshotPort).fetch();
    }
}
