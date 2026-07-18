package com.gqm2026.school.application;

import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招生资源（高中/初中校/控制线）应用服务。
 * 仅做编排与持久化，业务规则内聚于实体/领域服务；controller 只委托本服务。
 */
@Service
@RequiredArgsConstructor
public class SchoolAdminAppService {

    private final HighSchoolRepository highSchoolRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;
    private final ControlLineRepository controlLineRepository;

    @Transactional(readOnly = true)
    public Page<HighSchool> listHighSchools(Pageable pageable) {
        return highSchoolRepository.findAll(pageable);
    }

    @Transactional
    public HighSchool createHighSchool(HighSchool highSchool) {
        highSchool.setId(null);
        return highSchoolRepository.save(highSchool);
    }

    @Transactional
    public HighSchool updateHighSchool(Long id, HighSchool highSchool) {
        highSchool.setId(id);
        return highSchoolRepository.save(highSchool);
    }

    @Transactional
    public void deleteHighSchool(Long id) {
        highSchoolRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<JuniorSchool> listJuniorSchools(Pageable pageable) {
        return juniorSchoolRepository.findAll(pageable);
    }

    @Transactional
    public JuniorSchool createJuniorSchool(JuniorSchool juniorSchool) {
        juniorSchool.setId(null);
        return juniorSchoolRepository.save(juniorSchool);
    }

    @Transactional
    public JuniorSchool updateJuniorSchool(Long id, JuniorSchool juniorSchool) {
        juniorSchool.setId(id);
        return juniorSchoolRepository.save(juniorSchool);
    }

    @Transactional
    public void deleteJuniorSchool(Long id) {
        juniorSchoolRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ControlLine getControlLine(String type) {
        return controlLineRepository.findByType(type)
                .orElse(ControlLine.builder().type(type).value(0).build());
    }

    @Transactional
    public ControlLine upsertControlLine(ControlLine controlLine) {
        ControlLine existing = controlLineRepository.findByType(controlLine.getType()).orElse(null);
        if (existing != null) {
            existing.setValue(controlLine.getValue());
            return controlLineRepository.save(existing);
        }
        controlLine.setId(null);
        return controlLineRepository.save(controlLine);
    }
}
