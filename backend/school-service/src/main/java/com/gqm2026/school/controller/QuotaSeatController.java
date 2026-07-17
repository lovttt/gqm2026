package com.gqm2026.school.controller;

import com.gqm2026.school.config.QuotaGroupConfig;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class QuotaSeatController {

    private final QuotaSeatRepository quotaSeatRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;

    /** 共享校额名额的初中校分组（同 QuotaGroupConfig.JUNIOR_GROUPS） */
    private static final List<List<String>> JUNIOR_GROUPS = QuotaGroupConfig.JUNIOR_GROUPS;

    @GetMapping("/quota-seats")
    public Page<QuotaSeat> listQuotaSeats(
            @RequestParam(required = false) Long juniorSchoolId,
            @RequestParam(required = false) Long highSchoolId,
            Pageable pageable) {
        if (juniorSchoolId != null && highSchoolId != null) {
            return quotaSeatRepository.findByJuniorSchoolIdAndHighSchoolId(juniorSchoolId, highSchoolId, pageable);
        }
        if (juniorSchoolId != null) {
            return listForJuniorGroup(juniorSchoolId, pageable);
        }
        if (highSchoolId != null) {
            return listForHighSchoolGroup(highSchoolId, pageable);
        }
        return quotaSeatRepository.findAll(pageable);
    }

    /** 按高中查询：若某初中校属于共享分组，则把组内所有学校的名额合并到同一行（按组汇总）后返回 */
    private Page<QuotaSeat> listForHighSchoolGroup(Long highSchoolId, Pageable pageable) {
        List<QuotaSeat> all = quotaSeatRepository.findByHighSchoolId(highSchoolId);
        if (all.isEmpty()) {
            return Page.empty(pageable);
        }
        // 非组初中校：juniorSchoolId -> 单行；共享组：groupLabel -> 合并行
        Map<Long, QuotaSeat> single = new LinkedHashMap<>();
        Map<String, QuotaSeat> grouped = new LinkedHashMap<>();
        for (QuotaSeat s : all) {
            JuniorSchool js = juniorSchoolRepository.findById(s.getJuniorSchoolId()).orElse(null);
            List<String> group = js != null ? groupOf(js.getName()) : null;
            if (group != null && group.size() > 1) {
                String label = String.join(" / ", group);
                QuotaSeat merged = grouped.get(label);
                if (merged == null) {
                    Long repId = group.stream()
                            .map(juniorSchoolRepository::findByName)
                            .filter(Objects::nonNull)
                            .map(JuniorSchool::getId)
                            .findFirst()
                            .orElse(s.getJuniorSchoolId());
                    merged = QuotaSeat.builder()
                            .juniorSchoolId(repId)
                            .highSchoolId(highSchoolId)
                            .quota(0)
                            .build();
                    merged.setJuniorSchoolNames(label);
                    grouped.put(label, merged);
                }
                merged.setQuota(merged.getQuota() + s.getQuota());
            } else {
                QuotaSeat one = single.get(s.getJuniorSchoolId());
                if (one == null) {
                    one = QuotaSeat.builder()
                            .juniorSchoolId(s.getJuniorSchoolId())
                            .highSchoolId(highSchoolId)
                            .quota(s.getQuota())
                            .build();
                    single.put(s.getJuniorSchoolId(), one);
                } else {
                    one.setQuota(one.getQuota() + s.getQuota());
                }
            }
        }
        List<QuotaSeat> result = new ArrayList<>();
        result.addAll(single.values());
        result.addAll(grouped.values());
        return new PageImpl<>(result, pageable, result.size());
    }

    /** 按初中校查询：若该校属于共享分组，则合并组内所有学校的名额（按高中汇总）后返回 */
    private Page<QuotaSeat> listForJuniorGroup(Long juniorSchoolId, Pageable pageable) {
        JuniorSchool self = juniorSchoolRepository.findById(juniorSchoolId).orElse(null);
        if (self == null) {
            return quotaSeatRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
        }
        List<String> groupNames = groupOf(self.getName());
        if (groupNames == null || groupNames.size() <= 1) {
            return quotaSeatRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
        }
        // 组内所有存在的初中校 id
        List<Long> ids = groupNames.stream()
                .map(juniorSchoolRepository::findByName)
                .filter(Objects::nonNull)
                .map(JuniorSchool::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return quotaSeatRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
        }
        // 按高中合并名额
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (QuotaSeat s : quotaSeatRepository.findByJuniorSchoolIdIn(ids)) {
            merged.merge(s.getHighSchoolId(), s.getQuota(), Integer::sum);
        }
        // 展示顺序：被查询的校放最前
        List<String> ordered = new ArrayList<>(groupNames);
        ordered.remove(self.getName());
        ordered.add(0, self.getName());
        String label = String.join(" / ", ordered);
        List<QuotaSeat> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : merged.entrySet()) {
            QuotaSeat qs = QuotaSeat.builder()
                    .juniorSchoolId(juniorSchoolId)
                    .highSchoolId(e.getKey())
                    .quota(e.getValue())
                    .build();
            qs.setJuniorSchoolNames(label);
            result.add(qs);
        }
        return new PageImpl<>(result, pageable, result.size());
    }

    private List<String> groupOf(String name) {
        for (List<String> g : JUNIOR_GROUPS) {
            for (String m : g) {
                if (m.equals(name)) {
                    return g;
                }
            }
        }
        return null;
    }

    @PostMapping("/quota-seats")
    public QuotaSeat createQuotaSeat(@RequestBody QuotaSeat quotaSeat) {
        QuotaSeat existing = quotaSeatRepository
                .findByJuniorSchoolIdAndHighSchoolId(quotaSeat.getJuniorSchoolId(), quotaSeat.getHighSchoolId());
        if (existing != null) {
            existing.setQuota(quotaSeat.getQuota());
            return quotaSeatRepository.save(existing);
        }
        quotaSeat.setId(null);
        return quotaSeatRepository.save(quotaSeat);
    }

    @PutMapping("/quota-seats/{id}")
    public QuotaSeat updateQuotaSeat(@PathVariable Long id, @RequestBody QuotaSeat quotaSeat) {
        quotaSeat.setId(id);
        return quotaSeatRepository.save(quotaSeat);
    }

    @DeleteMapping("/quota-seats/{id}")
    public void deleteQuotaSeat(@PathVariable Long id) {
        quotaSeatRepository.deleteById(id);
    }
}
