package com.gqm2026.school.domain;

import com.gqm2026.school.config.QuotaGroupConfig;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校额名额分组合并领域服务（解 08 M3）。
 * 单一事实来源，替代原 QuotaSeatController 私有方法：
 * 同一共享分组（QuotaGroupConfig.JUNIOR_GROUPS）内的初中校名额按高中汇总合并展示。
 */
@Service
@RequiredArgsConstructor
public class QuotaGroupService {

    private final JuniorSchoolRepository juniorSchoolRepository;
    private final QuotaSeatRepository quotaSeatRepository;

    /** 按高中查询：若某初中校属于共享分组，则把组内所有学校的名额合并到同一行（按组汇总）后返回 */
    public Page<QuotaSeat> mergeForHighSchool(Long highSchoolId, List<QuotaSeat> all, Pageable pageable) {
        if (all.isEmpty()) {
            return Page.empty(pageable);
        }
        Map<Long, QuotaSeat> single = new LinkedHashMap<>();
        Map<String, QuotaSeat> grouped = new LinkedHashMap<>();
        for (QuotaSeat s : all) {
            JuniorSchool js = juniorSchoolRepository.findById(s.getJuniorSchoolId()).orElse(null);
            List<String> group = js != null ? groupOf(js.getName()) : null;
            if (group != null && group.size() > 1) {
                String label = String.join(" / ", group);
                QuotaSeat merged = grouped.get(label);
                if (merged == null) {
                    Map<String, Long> nameToId = juniorNameToId(group);
                    Long repId = group.stream().map(nameToId::get).filter(Objects::nonNull)
                            .findFirst().orElse(s.getJuniorSchoolId());
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
        List<QuotaSeat> result = new ArrayList<>(single.values());
        result.addAll(grouped.values());
        return new PageImpl<>(result, pageable, result.size());
    }

    /** 按初中校查询：若该校属于共享分组，则合并组内所有学校的名额（按高中汇总）后返回；否则返回 null 交由调用方回退精确查询 */
    public Page<QuotaSeat> mergeForJunior(Long juniorSchoolId, Pageable pageable) {
        JuniorSchool self = juniorSchoolRepository.findById(juniorSchoolId).orElse(null);
        if (self == null) {
            return null;
        }
        List<String> groupNames = groupOf(self.getName());
        if (groupNames == null || groupNames.size() <= 1) {
            return null;
        }
        Map<String, Long> nameToId = juniorNameToId(groupNames);
        List<Long> ids = groupNames.stream().map(nameToId::get).filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return null;
        }
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (QuotaSeat s : quotaSeatRepository.findByJuniorSchoolIdIn(ids)) {
            merged.merge(s.getHighSchoolId(), s.getQuota(), Integer::sum);
        }
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

    /** 返回包含给定校名的共享分组；不属于任何分组则返回 null */
    public List<String> groupOf(String name) {
        for (List<String> g : QuotaGroupConfig.JUNIOR_GROUPS) {
            for (String m : g) {
                if (m.equals(name)) {
                    return g;
                }
            }
        }
        return null;
    }

    private Map<String, Long> juniorNameToId(List<String> names) {
        return juniorSchoolRepository.findByNameIn(names).stream()
                .collect(Collectors.toMap(JuniorSchool::getName, JuniorSchool::getId, (a, b) -> a));
    }
}
