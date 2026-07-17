package com.gqm2026.admission.controller;

import com.gqm2026.admission.engine.AdmissionEngine;
import com.gqm2026.admission.entity.AdmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** 录取控制器契约测试：验证 /runs、/runs/{id}、/run/full 等端点返回形态（对应 04 §4.5、03 §3.8） */
@ExtendWith(MockitoExtension.class)
class AdmissionControllerTest {

    @Mock
    private AdmissionEngine engine;

    @InjectMocks
    private AdmissionController controller;

    @Test
    void runsEndpointReturnsHistoryList() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("runId", 1L);
        entry.put("total", 2L);
        entry.put("admitted", 2L);
        entry.put("notAdmitted", 0L);
        entry.put("quotaAdmitted", 1L);
        entry.put("tongzhaoAdmitted", 1L);
        when(engine.runs()).thenReturn(List.of(entry));

        List<Map<String, Object>> runs = controller.runs();

        assertEquals(1, runs.size());
        assertEquals(1L, runs.get(0).get("runId"));
    }

    @Test
    void runsByIdDelegatesToEngine() {
        AdmissionResult ar = AdmissionResult.builder()
                .studentId(1L).batch("QUOTA").status("ADMITTED").build();
        when(engine.resultsByRun(7L)).thenReturn(List.of(ar));

        List<AdmissionResult> res = controller.resultsByRun(7L);

        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).getStudentId());
    }

    @Test
    void runFullReturnsStatsWithRunId() {
        when(engine.runFull()).thenReturn(Map.of("runId", 5L, "admitted", 1L));

        Map<String, Object> stats = controller.runFull();

        assertEquals(5L, stats.get("runId"));
    }
}
