package com.gqm2026.school.application;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 招生资源导入导出应用服务。
 * export 组装 SchoolDataset；import 先删后写（含 quotaGroups 由 QuotaGroupConfig 派生）。
 */
@Service
@RequiredArgsConstructor
public class SchoolDatasetAppService {

    private final HighSchoolRepository highSchoolRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;
    private final QuotaSeatRepository quotaSeatRepository;
    private final ControlLineRepository controlLineRepository;
    private final ScoreLineRepository scoreLineRepository;
    private final ScoreSegmentRepository scoreSegmentRepository;

    @Transactional(readOnly = true)
    public SchoolDataset exportDataset() {
        ControlLine controlLine = controlLineRepository.findByType("QUOTA").orElse(null);
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

    @Transactional
    public String importDataset(SchoolDataset dataset) {
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
