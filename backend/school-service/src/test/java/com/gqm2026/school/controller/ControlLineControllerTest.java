package com.gqm2026.school.controller;

import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 控制线（校额到校 430）读取/upsert 契约测试（纯 Mock，对应 02/03，阶段1） */
@ExtendWith(MockitoExtension.class)
class ControlLineControllerTest {

    @Mock private ControlLineRepository controlLineRepository;
    @Mock private HighSchoolRepository highSchoolRepository;
    @Mock private JuniorSchoolRepository juniorSchoolRepository;

    @InjectMocks
    private SchoolController controller;

    @Test
    void getControlLine_returnsSeededValue() {
        when(controlLineRepository.findByType("QUOTA"))
                .thenReturn(Optional.of(ControlLine.builder().type("QUOTA").value(430).build()));
        ControlLine cl = controller.getControlLine("QUOTA");
        assertEquals(430, cl.getValue());
    }

    @Test
    void getControlLine_missingType_returnsZeroDefault() {
        when(controlLineRepository.findByType("X")).thenReturn(Optional.empty());
        ControlLine cl = controller.getControlLine("X");
        assertEquals(0, cl.getValue());
        assertEquals("X", cl.getType());
    }

    @Test
    void upsertControlLine_newType_creates() {
        when(controlLineRepository.findByType("NEW")).thenReturn(Optional.empty());
        when(controlLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ControlLine cl = controller.upsertControlLine(ControlLine.builder().type("NEW").value(500).build());
        assertEquals(500, cl.getValue());
    }

    @Test
    void upsertControlLine_existingType_updatesValue() {
        ControlLine existing = ControlLine.builder().id(1L).type("QUOTA").value(430).build();
        when(controlLineRepository.findByType("QUOTA")).thenReturn(Optional.of(existing));
        when(controlLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ControlLine cl = controller.upsertControlLine(ControlLine.builder().type("QUOTA").value(450).build());
        assertEquals(450, cl.getValue());
        assertEquals(1L, cl.getId());
    }
}
