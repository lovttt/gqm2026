package com.gqm2026.school.application;

import com.gqm2026.school.entity.ControlLine;
import com.gqm2026.school.entity.GaokaoTier;
import com.gqm2026.school.entity.HighSchool;
import com.gqm2026.school.entity.JuniorSchool;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.entity.ScoreLine;
import com.gqm2026.school.entity.ScoreSegment;
import com.gqm2026.school.repository.ControlLineRepository;
import com.gqm2026.school.repository.HighSchoolRepository;
import com.gqm2026.school.repository.JuniorSchoolRepository;
import com.gqm2026.school.repository.QuotaSeatRepository;
import com.gqm2026.school.repository.ScoreLineRepository;
import com.gqm2026.school.repository.ScoreSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 招生资源种子数据应用服务（G7-Q2）。
 * seedIfEmpty：首次启动播种高中/初中/校额/控制线/统招线，并按 tier 派生 gaokaoTier；
 * 已播种时幂等回填初中校班数/毕业生数与高中 gaokaoTier。
 * CommandLineRunner（SeedDataService）仅委托本服务，保持应用层与基础设施分离。
 */
@Service
@RequiredArgsConstructor
public class SeedDataAppService {

    private final HighSchoolRepository highSchoolRepository;
    private final JuniorSchoolRepository juniorSchoolRepository;
    private final QuotaSeatRepository quotaSeatRepository;
    private final ControlLineRepository controlLineRepository;
    private final ScoreLineRepository scoreLineRepository;
    private final ScoreSegmentRepository scoreSegmentRepository;

    @Value("${app.seed-dir:../data}")
    private String seedDir;

    /** 2025 统招线 CSV 中的简称 -> 高中全称（数据来源：2026-1.xlsx 的 20255 sheet） */
    private static final Map<String, String> LINE_SCHOOL_MAP = Map.ofEntries(
            Map.entry("二中", "北京市第二中学"),
            Map.entry("汇文", "北京汇文中学"),
            Map.entry("广中", "北京市广渠门中学"),
            Map.entry("五中", "北京市第五中学"),
            Map.entry("景山", "北京景山学校"),
            Map.entry("龙潭", "北京市龙潭中学"),
            Map.entry("东直门", "北京市东直门中学"),
            Map.entry("166中", "北京市第一六六中学"),
            Map.entry("11中", "北京市第十一中学"),
            Map.entry("55中", "北京市第五十五中学"),
            Map.entry("汇文实验", "北京汇文实验中学"),
            Map.entry("50中", "北京市第五十中学"),
            Map.entry("165中", "北京市第一六五中学"),
            Map.entry("24中", "北京市第二十四中学"),
            Map.entry("171中", "北京市第一七一中学"),
            Map.entry("22中", "北京市第二十二中学"),
            Map.entry("109中", "北京市第一零九中学"),
            Map.entry("96中", "北京市第九十六中学"),
            Map.entry("65中", "北京市第六十五中学"),
            Map.entry("一中", "北京市第一中学"),
            Map.entry("142中", "北京市第一四二中学"),
            Map.entry("27中", "北京市第二十七中学"),
            Map.entry("25中", "北京市第二十五中学"),
            Map.entry("54中", "北京市第五十四中学"),
            Map.entry("21中", "北京市第二十一中学"),
            Map.entry("50中分校", "北京市第五十中学分校"),
            Map.entry("翔宇", "北京市翔宇中学")
    );

    @Transactional
    public void seedIfEmpty() throws Exception {
        seedSegmentsIfEmpty();
        if (!highSchoolRepository.findAll().isEmpty()) {
            // 已播种：仅幂等回填初中校班数/毕业生数 + 高中 gaokaoTier
            backfillJuniorSchoolStats();
            backfillHighSchoolGaokaoTier();
            return;
        }

        // 1) 高中（统招计划 + 层次 + 片区 + 高考出口梯队）
        Map<String, Long> hsIdByName = new HashMap<>();
        for (String[] row : readCsv(seedDir + "/high_school_seed.csv")) {
            HighSchool.HighSchoolBuilder builder = HighSchool.builder()
                    .name(row[0])
                    .tongzhaoQuota(Integer.parseInt(row[1].trim()))
                    .tier(row[2].trim())
                    .zone(Integer.parseInt(row[3].trim()))
                    .gaokaoTier(gaokaoTierOf(row[2].trim()));
            if (row.length > 4 && !row[4].trim().isEmpty()) {
                builder.code(row[4].trim());
            }
            HighSchool hs = highSchoolRepository.save(builder.build());
            hsIdByName.put(norm(row[0]), hs.getId());
        }

        // 2) 初中校（片区）
        Map<String, Long> jsIdByName = new HashMap<>();
        for (String[] row : readCsv(seedDir + "/junior_school_seed.csv")) {
            JuniorSchool js = juniorSchoolRepository.save(JuniorSchool.builder()
                    .name(row[0])
                    .zone(Integer.parseInt(row[2].trim()))
                    .build());
            jsIdByName.put(norm(row[0]), js.getId());
        }

        // 3) 校额到校名额（初中校 -> 高中 -> 名额）
        int quotaRows = 0;
        Map<Long, Integer> hsQuotaTotal = new HashMap<>();
        for (String[] row : readCsv(seedDir + "/quota_seat_real.csv")) {
            Long jsId = jsIdByName.get(norm(row[0]));
            Long hsId = hsIdByName.get(norm(row[1]));
            if (jsId == null || hsId == null) {
                System.out.println("[school-service] 跳过未知校额行: " + Arrays.toString(row));
                continue;
            }
            int q = Integer.parseInt(row[2].trim());
            quotaSeatRepository.save(QuotaSeat.builder()
                    .juniorSchoolId(jsId).highSchoolId(hsId)
                    .quota(q).build());
            hsQuotaTotal.merge(hsId, q, Integer::sum);
            quotaRows++;
        }

        // 3.1) 将每所高中的「校额录取数」默认填成其校额到校名额总数（与「校额到校分配数」对齐，可在高中管理页编辑）
        for (HighSchool hs : highSchoolRepository.findAll()) {
            int total = hsQuotaTotal.getOrDefault(hs.getId(), 0);
            if (hs.getQuotaAdmitted() != total) {
                hs.setQuotaAdmitted(total);
                highSchoolRepository.save(hs);
            }
        }

        // 4) 校额到校全区最低控制线（430 分以上具备资格）
        controlLineRepository.save(ControlLine.builder().type("QUOTA").value(430).build());

        // 5) 2025 统招录取线（取各校普通班统招线；数据来自 2026-1.xlsx 的 20255 sheet）
        Map<String, Integer> perSchool = new HashMap<>();
        for (String[] row : readCsv(seedDir + "/score_line_2025_raw.csv")) {
            String full = LINE_SCHOOL_MAP.get(row[0].trim());
            if (full == null) continue;
            int sc = Integer.parseInt(row[2].trim());
            perSchool.merge(full, sc, Math::min);
        }
        int lineRows = 0;
        for (Map.Entry<String, Integer> e : perSchool.entrySet()) {
            Long hsId = hsIdByName.get(e.getKey());
            if (hsId == null) continue;
            scoreLineRepository.save(ScoreLine.builder()
                    .highSchoolId(hsId).year(2025).batch("TONGZHAO").score(e.getValue()).build());
            lineRows++;
        }

        // 首次播种后回填初中校班数/毕业生数 + 高中 gaokaoTier
        backfillJuniorSchoolStats();
        backfillHighSchoolGaokaoTier();

        System.out.println("[school-service] 种子数据初始化完成: 高中=" + hsIdByName.size()
                + " 初中=" + jsIdByName.size() + " 校额=" + quotaRows
                + " 2025线=" + lineRows + " 控制线=430");
    }

    /** 按 junior_school.csv（初中校,班数,毕业生数）回填初中校的 classCount / gradCount，按名称对齐，幂等 */
    private void backfillJuniorSchoolStats() throws Exception {
        List<JuniorSchool> all = juniorSchoolRepository.findAll();
        if (all.isEmpty()) return;
        Map<String, JuniorSchool> byName = new HashMap<>();
        for (JuniorSchool js : all) byName.put(js.getName(), js);
        int updated = 0;
        for (String[] row : readCsv(seedDir + "/junior_school.csv")) {
            JuniorSchool js = byName.get(row[0].trim());
            if (js == null) continue;
            int cc = Integer.parseInt(row[1].trim());
            int gc = Integer.parseInt(row[2].trim());
            if (js.getClassCount() != cc || js.getGradCount() != gc) {
                js.setClassCount(cc);
                js.setGradCount(gc);
                juniorSchoolRepository.save(js);
                updated++;
            }
        }
        if (updated > 0) System.out.println("[school-service] 回填初中校班数/毕业生数: " + updated + " 所");
    }

    /** 幂等回填高中 gaokaoTier：已播种但 gaokaoTier 为空时按 tier 派生（KEY→TOP，NORMAL→MID） */
    private void backfillHighSchoolGaokaoTier() {
        for (HighSchool hs : highSchoolRepository.findAll()) {
            if (hs.getGaokaoTier() == null) {
                hs.setGaokaoTier(gaokaoTierOf(hs.getTier()));
                highSchoolRepository.save(hs);
            }
        }
    }

    /** 若分段表未播种，则按年导入 2025/2026 一分一段表（分数 -> 区排名），供志愿模拟做排名换算 */
    private void seedSegmentsIfEmpty() {
        if (!scoreSegmentRepository.findAll().isEmpty()) return;
        seedSegmentsForYear(2025, seedDir + "/score_segment_2025.csv");
        seedSegmentsForYear(2026, seedDir + "/score_segment_2026.csv");
    }

    private void seedSegmentsForYear(int year, String path) {
        try {
            for (String[] row : readCsv(path)) {
                String label = row[0].trim();
                int score;
                if (label.contains("及以上")) score = 500;
                else if (label.contains("以下")) continue; // 300分以下 不纳入排名映射
                else score = Integer.parseInt(label);
                Integer head = (row.length > 1 && !row[1].trim().isEmpty()) ? Integer.parseInt(row[1].trim()) : null;
                Integer cum = (row.length > 2 && !row[2].trim().isEmpty()) ? Integer.parseInt(row[2].trim()) : null;
                if (cum == null) continue;
                scoreSegmentRepository.save(ScoreSegment.builder()
                        .year(year).score(score)
                        .headcount(head == null ? 0 : head)
                        .cumulative(cum).build());
            }
        } catch (Exception e) {
            System.out.println("[school-service] 分段表播种失败 " + path + " : " + e.getMessage());
        }
    }

    /** 由学校层次派生育高考出口梯队（KEY→TOP，其余→MID；HEAD 由管理员手动设定） */
    private static GaokaoTier gaokaoTierOf(String tier) {
        return "KEY".equalsIgnoreCase(tier) ? GaokaoTier.TOP : GaokaoTier.MID;
    }

    /** 规范化校名：去掉括号注释（如「(北京市第一二五中学)」）及首尾空白，便于跨表匹配 */
    private static String norm(String name) {
        if (name == null) return null;
        String s = name.trim();
        int idx = s.indexOf('(');
        if (idx < 0) idx = s.indexOf('（');
        if (idx >= 0) s = s.substring(0, idx);
        return s.trim();
    }

    /** 读取 CSV（跳过表头、去 BOM、按逗号切分） */
    private List<String[]> readCsv(String path) throws Exception {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.isBlank()) continue;
                if (line.charAt(0) == '﻿') line = line.substring(1);
                out.add(line.split(","));
            }
        }
        return out;
    }
}
