package com.gqm2026.student.simulator;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.repository.ApplicationRepository;
import com.gqm2026.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 考生志愿模拟器（拟真考生策略）。
 *
 * 设计约束见 docs/spec/07-simulator.md。核心 {@link #simulate} 为纯函数，依据考生自身情况生成志愿：
 *  - 校额到校：仅「具备资格且过控制线」者填报，对口高中按 层次优先 + 离家近优先 排序；
 *  - 统招平行志愿：仅「达到普高线(=控制线)」者填报，按 冲/稳/保 三档，并以「区排名」拉开档次：
 *      * 用 2026 一分一段表把考生分数换算成区排名；
 *      * 用 2025 一分一段表把各校 2025 统招线换算成区排名（= Excel「25年统招区排名」）；
 *      * 按「学校录取排名 − 考生排名」的差距分档：差距为负=冲(学校更难)，接近0=稳，差距为正=保；
 *      * 拉开档次（冲≤3、稳≤3、保≤2，共≤8），不盲目全填重点校——普通校按排名自然落入稳/保档；
 *      * 若最终 8 个志愿全是重点校，则强制加入最接近的一所普通校作保底（贴近真实考生行为）。
 *
 * 因子：总分(必选) + 校额资格/初中校 + 区域(片区距离) + 学校层次。
 */
@Service
@RequiredArgsConstructor
public class ApplicationSimulator {

    private final SchoolReferencePort schoolReferencePort;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;

    /** 冲/稳/保 分档阈值（单位：区排名名次，数值越小越靠前） */
    private static final int REACH_FAR = 700;   // 比考生高 700 名以上：太冒险，不填
    private static final int REACH_NEAR = 60;   // 仅高 60 名以内视为「稳」而非「冲」
    private static final int MATCH_BAND = 400;  // 比考生低 400 名以内视为「稳/匹配」
    private static final int SAFETY_FAR = 1100; // 比考生低 1100 名以上：过于保底，不填

    /** 为所有「未提交锁定」的考生重新生成志愿，返回生成的志愿总数。
     *  批量收集后一次性写入，避免逐考生长事务导致的 SQLite 写锁争用。 */
    @Transactional
    public int regenerateAll() {
        ReferenceData ref = schoolReferencePort.fetch();
        List<Student> students = studentRepository.findAll();
        List<Application> all = new ArrayList<>();
        for (Student s : students) {
            if (s.isSubmitted()) continue; // 遵守提交锁：已提交者不覆盖
            all.addAll(simulate(s, ref));
        }
        applicationRepository.deleteAll();
        int generated = 0;
        int chunk = 1000;
        for (int i = 0; i < all.size(); i += chunk) {
            int end = Math.min(all.size(), i + chunk);
            generated += applicationRepository.saveAll(all.subList(i, end)).size();
        }
        applicationRepository.flush();
        return generated;
    }

    /** 纯函数：依据考生自身情况 + 参考数据，生成志愿列表（可单测） */
    public List<Application> simulate(Student s, ReferenceData ref) {
        List<Application> apps = new ArrayList<>();
        int score = s.getTotalScore();
        int controlLine = ref.controlLine;
        Long jsId = s.getJuniorSchoolId();
        int jsZone = ref.juniorZoneById.getOrDefault(jsId, 1);

        // 1) 校额到校志愿
        if (s.isHasQuotaEligibility() && score >= controlLine) {
            List<Long> quotaHs = new ArrayList<>(
                    ref.quotaHighSchoolsByJs.getOrDefault(jsId, List.of()));
            quotaHs.sort((a, b) -> {
                ReferenceData.HighSchoolView ha = ref.hsById.get(a);
                ReferenceData.HighSchoolView hb = ref.hsById.get(b);
                int c = tierRank(ha.tier()) - tierRank(hb.tier());
                if (c != 0) return c;
                return Integer.compare(distance(jsZone, ha.zone()), distance(jsZone, hb.zone()));
            });
            int p = 1;
            for (Long hsId : quotaHs) {
                if (p > 10) break; // 校额到校意向志愿最多 10 个
                apps.add(Application.builder().studentId(s.getId())
                        .batch("QUOTA").priority(p++).highSchoolId(hsId).build());
            }
        }

        // 2) 统招平行志愿（冲/稳/保，按区排名拉开档次）
        if (score >= controlLine) {
            int studentRank = ref.cumulativeRank(2026, score);
            List<Ranked> reach = new ArrayList<>();
            List<Ranked> match = new ArrayList<>();
            List<Ranked> safety = new ArrayList<>();
            for (ReferenceData.HighSchoolView h : ref.highSchools) {
                int line = ref.line2025ById.getOrDefault(h.id(), estimateLine(h, controlLine));
                int schoolRank = ref.cumulativeRank(2025, line);
                int diff = schoolRank - studentRank; // <0: 学校录取排名高于考生(更难)
                if (diff < -REACH_FAR || diff > SAFETY_FAR) continue; // 远超能力 / 过于保底 不填
                Ranked r = new Ranked(h, diff);
                if (diff <= -REACH_NEAR) reach.add(r);
                else if (diff <= MATCH_BAND) match.add(r);
                else safety.add(r);
            }
            // 排序：冲——最接近考生的冲在前；稳/保——差距最小者在前
            reach.sort(Comparator.comparingInt((Ranked r) -> r.diff).reversed());
            match.sort(Comparator.comparingInt((Ranked r) -> Math.abs(r.diff)));
            safety.sort(Comparator.comparingInt((Ranked r) -> Math.abs(r.diff)));

            // 配额：冲≤3、稳≤3、保≤2，拉开档次（共≤8）
            List<Ranked> picked = new ArrayList<>();
            addCapped(reach, 3, picked);
            addCapped(match, 3, picked);
            addCapped(safety, 2, picked);
            // 兜底：若 8 个志愿全是重点校，强制加入最接近的一所普通校作保底
            if (picked.stream().allMatch(r -> "KEY".equals(r.h.tier()))) {
                Ranked closestNormal = safety.stream()
                        .filter(r -> "NORMAL".equals(r.h.tier()))
                        .min(Comparator.comparingInt(r -> Math.abs(r.diff))).orElse(null);
                if (closestNormal != null && picked.size() < 8) {
                    picked.add(closestNormal);
                }
            }
            int p = 1;
            for (Ranked r : picked) {
                apps.add(Application.builder().studentId(s.getId())
                        .batch("TONGZHAO").priority(p++).highSchoolId(r.h.id()).build());
            }
        }
        return apps;
    }

    /** 缺 2025 线学校：回退到 控制线 + 层次加成 + 计划竞争 启发式，再换算区排名 */
    private int estimateLine(ReferenceData.HighSchoolView h, int controlLine) {
        int tierBoost = "KEY".equals(h.tier()) ? 40 : 0;
        int compBoost = Math.max(0, (120 - h.tongzhaoQuota())) / 4;
        return controlLine + tierBoost + compBoost;
    }

    private void addCapped(List<Ranked> src, int max, List<Ranked> dst) {
        int n = Math.min(max, src.size());
        for (int i = 0; i < n; i++) dst.add(src.get(i));
    }

    private int tierRank(String tier) { return "KEY".equals(tier) ? 0 : 1; }
    private int distance(int a, int b) { return Math.abs(a - b); }

    private static class Ranked {
        final ReferenceData.HighSchoolView h;
        final int diff;
        Ranked(ReferenceData.HighSchoolView h, int diff) { this.h = h; this.diff = diff; }
    }
}
