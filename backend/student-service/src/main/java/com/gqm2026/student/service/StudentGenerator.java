package com.gqm2026.student.service;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.simulator.SchoolDataFetcher;
import com.gqm2026.student.simulator.SchoolDataset;
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
 * 考生生成器：基于「2023 年小升初」各班数 × 班额 估算每所初中校的考生数并批量生成。
 *
 * 数据来源：
 *  - {@code junior_school.csv}（初中校,班数,毕业生数）：取「班数」列；
 *  - {@code score_segment_2026.csv}（2026 一分一段表，334~500）：作为全区分数分布模板；
 *  - school-service 的初中校（经 {@link SchoolDataFetcher} 拉取）：按 name 对齐 juniorSchoolId。
 *
 * 估算口径：每校考生数 = 班数 × 班额（默认 40，北京标准班额）。分数从一分一段表按比例抽取，
 * 因此全区分数分布与真实一致；最低分为一分一段表下限（334）。
 */
@Service
@RequiredArgsConstructor
public class StudentGenerator {

    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final SchoolDataFetcher schoolDataFetcher;

    @Value("${app.seed-dir:../data}")
    private String seedDir;

    @Value("${app.per-class:40}")
    private int perClass;

    /** 按默认班额(40)生成 */
    @Transactional
    public int generate() {
        return generate(perClass);
    }

    /** 按指定班额生成：先清空旧考生与志愿，再逐校生成 班数×perClass 名考生 */
    @Transactional
    public int generate(int perClass) {
        // 1) 初中校 name -> id（优先 school-service，不可达回退本地种子顺序）
        Map<String, Long> jsIdByName = resolveJuniorSchoolIds();

        // 2) 班数（junior_school.csv：初中校,班数,毕业生数）
        Map<String, Integer> classesByName = new HashMap<>();
        int totalClasses = 0;
        for (String[] r : readCsv(seedDir + "/junior_school.csv")) {
            String name = r[0].trim();
            if (isSpecialEd(name)) continue;            // 特殊教育学校不参加中考统招
            int c = Integer.parseInt(r[1].trim());
            classesByName.put(name, c);
            totalClasses += c;
        }
        if (totalClasses == 0) {
            System.out.println("[student-service] 未读取到班数，生成中止");
            return 0;
        }

        // 3) 分数模板：2026 一分一段表(334~500)
        List<Integer> template = new ArrayList<>();
        for (String[] row : readCsv(seedDir + "/score_segment_2026.csv")) {
            String label = row[0].trim();
            int score;
            if (label.contains("及以上")) score = 500;
            else if (label.contains("以下")) continue;  // 300 分以下不纳入
            else score = Integer.parseInt(label);
            int count = Integer.parseInt(row[1].trim());
            for (int i = 0; i < count; i++) template.add(score);
        }
        Collections.shuffle(template, new Random(2026));

        // 4) 逐校生成：考生数 = 班数 × 班额
        applicationRepository.deleteAll();
        studentRepository.deleteAll();
        Random rnd = new Random(2026);
        int pos = 0, seq = 0, total = 0;
        List<Student> batch = new ArrayList<>();
        for (Map.Entry<String, Integer> e : classesByName.entrySet()) {
            Long jsId = jsIdByName.get(e.getKey());
            if (jsId == null) {
                System.out.println("[student-service] 跳过未匹配初中校(无id): " + e.getKey());
                continue;
            }
            int n = e.getValue() * perClass;
            for (int k = 0; k < n; k++) {
                int score = template.get(pos % template.size());
                pos++;
                int[] sub = splitSubjects(score, rnd);
                Student s = Student.builder()
                        .name("考生" + (1000 + seq))
                        .ticketNo(String.format("T2026%05d", seq))
                        .juniorSchoolId(jsId)
                        .chinese(sub[0]).math(sub[1]).english(sub[2])
                        .physics(sub[3]).politics(sub[4]).pe(sub[5])
                        .hasQuotaEligibility(score >= 430)
                        .build();
                batch.add(s); seq++; total++;
                if (batch.size() >= 1000) {
                    studentRepository.saveAll(batch);
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) studentRepository.saveAll(batch);

        System.out.println("[student-service] 按班数生成考生完成: 班数合计=" + totalClasses
                + " 班额=" + perClass + " 考生=" + total
                + "；志愿请调用 POST /student/applications/simulate 由模拟器生成");
        return total;
    }

    /** 解析初中校 name->id：优先 school-service（按 name 对齐），失败回退本地种子顺序 i+1 */
    private Map<String, Long> resolveJuniorSchoolIds() {
        try {
            SchoolDataset sd = schoolDataFetcher.fetchRaw();
            Map<String, Long> m = new HashMap<>();
            if (sd.juniorSchools != null) {
                for (SchoolDataset.JuniorSchoolInfo j : sd.juniorSchools) m.put(j.name, j.id);
            }
            if (!m.isEmpty()) return m;
        } catch (Exception ex) {
            System.out.println("[student-service] school-service 不可达，回退本地初中校 id 对齐: " + ex.getMessage());
        }
        // 回退：junior_school_seed.csv 顺序 i+1
        Map<String, Long> m = new HashMap<>();
        try {
            List<String[]> seed = readCsv(seedDir + "/junior_school_seed.csv");
            for (int i = 0; i < seed.size(); i++) m.put(seed.get(i)[0].trim(), (long) (i + 1));
        } catch (Exception ex) {
            System.out.println("[student-service] 本地种子读取失败: " + ex.getMessage());
        }
        return m;
    }

    /** 将总分拆分为 6 科（510 量纲：语数英各100、物80、道法80、体育50），保证合计=score */
    private int[] splitSubjects(int total, Random rnd) {
        int pe = 50;
        int politics = 80 - (rnd.nextInt(10) == 0 ? 1 + rnd.nextInt(8) : 0);
        int physics = 80 - (rnd.nextInt(10) == 0 ? 1 + rnd.nextInt(8) : 0);
        int remain = total - pe - politics - physics;   // 220..300
        if (remain < 0) remain = 0;
        int base = remain / 3;
        int chinese = clamp(base + (rnd.nextInt(11) - 5), 0, 100);
        int math = clamp(base + (rnd.nextInt(11) - 5), 0, 100);
        int english = remain - chinese - math;
        if (english < 0) {
            english = 0;
            chinese = clamp(remain - math, 0, 100);
            english = remain - chinese - math;
        } else if (english > 100) {
            english = 100;
            chinese = clamp(remain - math - 100, 0, 100);
            english = remain - chinese - math;
        }
        return new int[]{chinese, math, english, physics, politics, pe};
    }

    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private boolean isSpecialEd(String name) {
        return name.contains("特殊教育");
    }

    /** 读取 CSV（跳过表头、去 BOM、按逗号切分；读取失败返回空列表） */
    private List<String[]> readCsv(String path) {
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
        } catch (Exception e) {
            System.out.println("[student-service] 读取CSV失败 " + path + " : " + e.getMessage());
        }
        return out;
    }
}
