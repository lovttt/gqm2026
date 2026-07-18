package com.gqm2026.school.controller;

import com.gqm2026.school.application.SchoolDatasetAppService;
import com.gqm2026.school.dto.SchoolDataset;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class DataIoController {

    private final SchoolDatasetAppService schoolDatasetAppService;

    @GetMapping("/export")
    public SchoolDataset exportDataset() {
        return schoolDatasetAppService.exportDataset();
    }

    @PostMapping("/import")
    public String importDataset(@RequestBody SchoolDataset dataset) {
        return schoolDatasetAppService.importDataset(dataset);
    }
}
