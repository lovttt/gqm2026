package com.gqm2026.school.controller;

import com.gqm2026.school.application.SchoolAdminAppService;
import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolAdminAppService schoolAdminAppService;

    // ---------- 高中 ----------
    @GetMapping("/high-schools")
    public Page<HighSchool> listHighSchools(Pageable pageable) {
        return schoolAdminAppService.listHighSchools(pageable);
    }

    @PostMapping("/high-schools")
    public HighSchool createHighSchool(@RequestBody HighSchool highSchool) {
        return schoolAdminAppService.createHighSchool(highSchool);
    }

    @PutMapping("/high-schools/{id}")
    public HighSchool updateHighSchool(@PathVariable Long id, @RequestBody HighSchool highSchool) {
        return schoolAdminAppService.updateHighSchool(id, highSchool);
    }

    @DeleteMapping("/high-schools/{id}")
    public void deleteHighSchool(@PathVariable Long id) {
        schoolAdminAppService.deleteHighSchool(id);
    }

    // ---------- 初中校 ----------
    @GetMapping("/junior-schools")
    public Page<JuniorSchool> listJuniorSchools(Pageable pageable) {
        return schoolAdminAppService.listJuniorSchools(pageable);
    }

    @PostMapping("/junior-schools")
    public JuniorSchool createJuniorSchool(@RequestBody JuniorSchool juniorSchool) {
        return schoolAdminAppService.createJuniorSchool(juniorSchool);
    }

    @PutMapping("/junior-schools/{id}")
    public JuniorSchool updateJuniorSchool(@PathVariable Long id, @RequestBody JuniorSchool juniorSchool) {
        return schoolAdminAppService.updateJuniorSchool(id, juniorSchool);
    }

    @DeleteMapping("/junior-schools/{id}")
    public void deleteJuniorSchool(@PathVariable Long id) {
        schoolAdminAppService.deleteJuniorSchool(id);
    }

    // ---------- 全区最低控制线（校额到校）----------
    @GetMapping("/control-line")
    public ControlLine getControlLine(@RequestParam(defaultValue = "QUOTA") String type) {
        return schoolAdminAppService.getControlLine(type);
    }

    @PostMapping("/control-line")
    public ControlLine upsertControlLine(@RequestBody ControlLine controlLine) {
        return schoolAdminAppService.upsertControlLine(controlLine);
    }
}
