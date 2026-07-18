package com.gqm2026.school.controller;

import com.gqm2026.school.application.QuotaSeatAppService;
import com.gqm2026.school.entity.QuotaSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class QuotaSeatController {

    private final QuotaSeatAppService quotaSeatAppService;

    @GetMapping("/quota-seats")
    public Page<QuotaSeat> listQuotaSeats(
            @RequestParam(required = false) Long juniorSchoolId,
            @RequestParam(required = false) Long highSchoolId,
            Pageable pageable) {
        return quotaSeatAppService.listQuotaSeats(juniorSchoolId, highSchoolId, pageable);
    }

    @PostMapping("/quota-seats")
    public QuotaSeat createQuotaSeat(@RequestBody QuotaSeat quotaSeat) {
        return quotaSeatAppService.createOrUpdateQuotaSeat(quotaSeat);
    }

    @PutMapping("/quota-seats/{id}")
    public QuotaSeat updateQuotaSeat(@PathVariable Long id, @RequestBody QuotaSeat quotaSeat) {
        return quotaSeatAppService.updateQuotaSeat(id, quotaSeat);
    }

    @DeleteMapping("/quota-seats/{id}")
    public void deleteQuotaSeat(@PathVariable Long id) {
        quotaSeatAppService.deleteQuotaSeat(id);
    }
}
