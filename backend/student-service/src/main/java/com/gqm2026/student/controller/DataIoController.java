package com.gqm2026.student.controller;

import com.gqm2026.student.application.StudentDatasetAppService;
import com.gqm2026.student.dto.StudentDataset;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class DataIoController {

    private final StudentDatasetAppService studentDatasetAppService;

    @GetMapping("/export")
    public StudentDataset exportDataset() {
        return studentDatasetAppService.exportDataset();
    }

    @PostMapping("/import")
    public String importDataset(@RequestBody StudentDataset dataset) {
        return studentDatasetAppService.importDataset(dataset);
    }

    /** CSV 批量导入考生：name,juniorSchoolId,chinese,math,english,physics,politics,pe,eligibility */
    @PostMapping(value = "/import/csv", consumes = "text/plain")
    public String importCsv(@RequestBody String csv) {
        return studentDatasetAppService.importCsv(csv);
    }
}
