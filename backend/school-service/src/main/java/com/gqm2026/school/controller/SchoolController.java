package com.gqm2026.school.controller;

import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class SchoolController {

    private final HighSchoolRepository highSchoolRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;
    private final ControlLineRepository controlLineRepository;

    // ---------- 高中 ----------
    @GetMapping("/high-schools")
    public Page<HighSchool> listHighSchools(Pageable pageable) {
        return highSchoolRepository.findAll(pageable);
    }

    @PostMapping("/high-schools")
    public HighSchool createHighSchool(@RequestBody HighSchool highSchool) {
        highSchool.setId(null);
        return highSchoolRepository.save(highSchool);
    }

    @PutMapping("/high-schools/{id}")
    public HighSchool updateHighSchool(@PathVariable Long id, @RequestBody HighSchool highSchool) {
        highSchool.setId(id);
        return highSchoolRepository.save(highSchool);
    }

    @DeleteMapping("/high-schools/{id}")
    public void deleteHighSchool(@PathVariable Long id) {
        highSchoolRepository.deleteById(id);
    }

    // ---------- 初中校 ----------
    @GetMapping("/junior-schools")
    public Page<JuniorSchool> listJuniorSchools(Pageable pageable) {
        return juniorSchoolRepository.findAll(pageable);
    }

    @PostMapping("/junior-schools")
    public JuniorSchool createJuniorSchool(@RequestBody JuniorSchool juniorSchool) {
        juniorSchool.setId(null);
        return juniorSchoolRepository.save(juniorSchool);
    }

    @PutMapping("/junior-schools/{id}")
    public JuniorSchool updateJuniorSchool(@PathVariable Long id, @RequestBody JuniorSchool juniorSchool) {
        juniorSchool.setId(id);
        return juniorSchoolRepository.save(juniorSchool);
    }

    @DeleteMapping("/junior-schools/{id}")
    public void deleteJuniorSchool(@PathVariable Long id) {
        juniorSchoolRepository.deleteById(id);
    }

    // ---------- 全区最低控制线（校额到校）----------
    @GetMapping("/control-line")
    public ControlLine getControlLine(@RequestParam(defaultValue = "QUOTA") String type) {
        return controlLineRepository.findByType(type)
                .orElse(ControlLine.builder().type(type).value(0).build());
    }

    @PostMapping("/control-line")
    public ControlLine upsertControlLine(@RequestBody ControlLine controlLine) {
        ControlLine existing = controlLineRepository.findByType(controlLine.getType())
                .orElse(null);
        if (existing != null) {
            existing.setValue(controlLine.getValue());
            return controlLineRepository.save(existing);
        }
        controlLine.setId(null);
        return controlLineRepository.save(controlLine);
    }
}
