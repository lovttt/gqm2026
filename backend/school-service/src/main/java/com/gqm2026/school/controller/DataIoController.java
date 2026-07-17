package com.gqm2026.school.controller;

import com.gqm2026.school.config.QuotaGroupConfig;
import com.gqm2026.school.dto.SchoolDataset;
import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import com.gqm2026.school.repository.ScoreLineRepository;
import com.gqm2026.school.repository.ScoreSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class DataIoController {

    private final HighSchoolRepository highSchoolRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;
    private final QuotaSeatRepository quotaSeatRepository;
    private final ControlLineRepository controlLineRepository;
    private final ScoreLineRepository scoreLineRepository;
    private final ScoreSegmentRepository scoreSegmentRepository;

    @GetMapping("/export")
    public SchoolDataset exportDataset() {
        ControlLine controlLine = controlLineRepository.findByType("QUOTA").orElse(null);
        // 共享校额名额的初中校分组（名字 -> 数据库 id）
        List<List<Long>> quotaGroups = QuotaGroupConfig.JUNIOR_GROUPS.stream()
                .map(groupNames -> groupNames.stream()
                        .map(juniorSchoolRepository::findByName)
                        .filter(Objects::nonNull)
                        .map(JuniorSchool::getId)
                        .collect(Collectors.toList()))
                .filter(ids -> !ids.isEmpty())
                .collect(Collectors.toList());
        return SchoolDataset.builder()
                .highSchools(highSchoolRepository.findAll())
                .juniorSchools(juniorSchoolRepository.findAll())
                .quotaSeats(quotaSeatRepository.findAll())
                .quotaGroups(quotaGroups)
                .controlLine(controlLine)
                .scoreLines(scoreLineRepository.findAll())
                .scoreSegments(scoreSegmentRepository.findAll())
                .build();
    }

    @PostMapping("/import")
    @Transactional
    public String importDataset(@RequestBody SchoolDataset dataset) {
        highSchoolRepository.deleteAll();
        juniorSchoolRepository.deleteAll();
        quotaSeatRepository.deleteAll();
        controlLineRepository.deleteAll();

        if (dataset.getHighSchools() != null) {
            dataset.getHighSchools().forEach(h -> h.setId(null));
            highSchoolRepository.saveAll(dataset.getHighSchools());
        }
        if (dataset.getJuniorSchools() != null) {
            dataset.getJuniorSchools().forEach(j -> j.setId(null));
            juniorSchoolRepository.saveAll(dataset.getJuniorSchools());
        }
        if (dataset.getQuotaSeats() != null) {
            dataset.getQuotaSeats().forEach(q -> q.setId(null));
            quotaSeatRepository.saveAll(dataset.getQuotaSeats());
        }
        if (dataset.getControlLine() != null) {
            dataset.getControlLine().setId(null);
            controlLineRepository.save(dataset.getControlLine());
        }
        return "imported";
    }
}
