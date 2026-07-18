package com.gqm2026.student.generator;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.generator.dto.*;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.infrastructure.acl.SchoolReferencePort;
import com.gqm2026.student.simulator.ReferenceData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static com.gqm2026.student.generator.GeneratorConstants.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 单考生志愿生成器：校验清单 + 生成方案 + 偏好权重联动对比。
 *
 * <p>覆盖用户清单的全部校验维度：
 * <ol>
 *   <li>通勤时长校验：以片区距离近似通勤分钟，过滤超过上限的学校</li>
 *   <li>高考出口优先级校验：按 TOP/HEAD/MID 梯队 + 可选跨区投放校匹配</li>
 *   <li>政策规则：志愿容量(QUOTA≤8/TONGZHAO≤12)、校额门槛(总分≥430 且评价≥B)、贯通门槛(总分≥380)</li>
 *   <li>权重调节：梯度权重总和=100%、梯度区间(冲刺+5~15 / 兜底−10~20)、偏好权重联动实时重排</li>
 * </ol>
 *
 * <p>数据模型相关字段（综合素质评价 / 贯通标识 / 跨区标记 / 高考出口梯队）当前按「接口占位」实现，
 * 见 {@link StudentAttributes}、{@link GaokaoTierResolver}、{@link CommuteEstimator}。
 */
@Service
@RequiredArgsConstructor
public class GeneratorService {

    private final StudentRepository studentRepository;
    private final SchoolReferencePort schoolReferencePort;
    private final CommuteEstimator commuteEstimator;
    private final GaokaoTierResolver gaokaoTierResolver;
    private final StudentAttributes studentAttributes;

    /** 通勤归一化上限（分钟），超过按最差计 */
    private static final int MAX_COMMUTE = 60;

    public GenerateResponse generate(GenerateRequest req) {
        Student s = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "考生不存在"));
        ReferenceData ref = schoolReferencePort.fetch();
        int controlLine = (ref.controlLine > 0) ? ref.controlLine : QUOTA_CONTROL_LINE;
        int score = s.getTotalScore();
        long jsId = s.getJuniorSchoolId() == null ? -1 : s.getJuniorSchoolId();
        int jsZone = ref.juniorZoneById.getOrDefault(jsId, 1);
        String compEval = studentAttributes.comprehensiveEval(s, req.getComprehensiveEval());

        List<ValidationIssue> issues = new ArrayList<>();

        // ===== 政策规则类：校额到校门槛 =====
        boolean quotaLocked = false;
        if (score < controlLine || !evalMeetsB(compEval)) {
            quotaLocked = true;
            List<String> reasons = new ArrayList<>();
            if (score < controlLine) reasons.add("总分" + score + "低于控制线" + controlLine);
            if (!evalMeetsB(compEval)) reasons.add("综合素质评价" + compEval + "未达B等");
            issues.add(ValidationIssue.builder()
                    .code("QUOTA_THRESHOLD").level("BLOCK").batch(BATCH_QUOTA)
                    .message("您不满足校额到校报考条件，无法填报该批次志愿（" + String.join("；", reasons) + "）")
                    .build());
        }

        // ===== 政策规则类：贯通项目门槛 =====
        boolean guantongHidden = false;
        if (score < GUANTONG_LINE) {
            guantongHidden = true;
            issues.add(ValidationIssue.builder()
                    .code("GUANTONG_THRESHOLD").level("WARN").batch(BATCH_GUANTONG)
                    .message("总分低于" + GUANTONG_LINE + "分，已屏蔽所有贯通培养项目志愿选项")
                    .build());
        }

        // ===== 权重调节类：梯度权重总和 =====
        int weightSum = req.getSprintWeight() + req.getSteadyWeight() + req.getSafetyWeight();
        if (weightSum != WEIGHT_SUM_TARGET) {
            issues.add(ValidationIssue.builder()
                    .code("WEIGHT_SUM").level("BLOCK").batch("GLOBAL")
                    .message("三类志愿权重总和需为100%，请调整数值（当前为" + weightSum + "%）")
                    .build());
        }

        // ===== 参数合法性：通勤上限 / 偏好权重范围 =====
        if (req.getCommuteCapMinutes() != null && req.getCommuteCapMinutes() < 0) {
            issues.add(ValidationIssue.builder()
                    .code("COMMUTE_CAP").level("BLOCK").batch("GLOBAL")
                    .message("通勤时长上限必须为非负整数")
                    .build());
        }
        if (!inRange(req.getCommuteWeight()) || !inRange(req.getGaokaoOutputWeight())) {
            issues.add(ValidationIssue.builder()
                    .code("PREF_WEIGHT").level("BLOCK").batch("GLOBAL")
                    .message("偏好权重（通勤距离 / 高考出口）须在 0~100 之间")
                    .build());
        }

        // ===== 低于普高线：无统招志愿 =====
        boolean belowLine = score < controlLine;
        if (belowLine && !quotaLocked) {
            issues.add(ValidationIssue.builder()
                    .code("BELOW_LINE").level("INFO").batch(BATCH_TONGZHAO)
                    .message("总分低于普高线(" + controlLine + ")，无法填报统一招生志愿")
                    .build());
        }

        // ===== 构建候选校属性（通勤 / 梯队 / 梯度区间 / 参考分） =====
        List<Candidate> all = new ArrayList<>();
        List<Long> filteredByCommute = new ArrayList<>();
        List<Long> filteredByGaokao = new ArrayList<>();
        for (ReferenceData.HighSchoolView h : ref.highSchools) {
            int refScore = ref.line2025ById.getOrDefault(h.id(), estimateLine(h, controlLine));
            int delta = refScore - score;
            int commute = commuteEstimator.estimateMinutes(jsId, jsZone, h.id(), h.zone());
            int zoneDist = Math.abs(jsZone - h.zone());
            GaokaoTier gt = gaokaoTierResolver.resolve(h);
            boolean isGuantong = studentAttributes.isGuantongSchool(h.id());
            boolean isCross = studentAttributes.isCrossDistrict(h.id());

            // 通勤过滤
            if (req.getCommuteCapMinutes() != null && commute > req.getCommuteCapMinutes()) {
                filteredByCommute.add(h.id());
                continue;
            }
            // 高考出口梯队过滤
            if (req.getGaokaoTierPref() != null && !req.getGaokaoTierPref().isBlank()) {
                boolean tierMatch = gt.name().equals(req.getGaokaoTierPref());
                boolean crossMatch = req.isIncludeCrossDistrict() && isCross;
                if (!tierMatch && !crossMatch) {
                    filteredByGaokao.add(h.id());
                    continue;
                }
            }
            all.add(new Candidate(h, refScore, delta, commute, zoneDist, gt, bandOf(delta), isGuantong));
        }

        // 各批次候选池
        List<Candidate> quotaPool = quotaLocked ? List.of()
                : all.stream().filter(c -> ref.quotaHighSchoolsByJs.getOrDefault(jsId, List.of()).contains(c.h.id()))
                .collect(Collectors.toList());
        List<Candidate> tongzhaoPool = belowLine ? List.of() : all;
        List<Candidate> guantongPool = guantongHidden ? List.of()
                : all.stream().filter(c -> c.isGuantong).collect(Collectors.toList());

        // 容量截断提示（候选多于上限 → 「无法新增」）
        capWarn(issues, BATCH_QUOTA, quotaPool.size(), QUOTA_CAP);
        capWarn(issues, BATCH_TONGZHAO, tongzhaoPool.size(), TONGZHAO_CAP);
        capWarn(issues, BATCH_GUANTONG, guantongPool.size(), GUANTONG_CAP);

        // ===== 生成方案（当前偏好权重） =====
        GradientWeights gw = new GradientWeights(req.getSprintWeight(), req.getSteadyWeight(), req.getSafetyWeight());
        List<GeneratedChoice> quotaPlan = buildPlan(quotaPool, BATCH_QUOTA, QUOTA_CAP, gw,
                req.getCommuteWeight(), req.getGaokaoOutputWeight());
        List<GeneratedChoice> tongzhaoPlan = buildPlan(tongzhaoPool, BATCH_TONGZHAO, TONGZHAO_CAP, gw,
                req.getCommuteWeight(), req.getGaokaoOutputWeight());
        List<GeneratedChoice> guantongPlan = buildPlan(guantongPool, BATCH_GUANTONG, GUANTONG_CAP, gw,
                req.getCommuteWeight(), req.getGaokaoOutputWeight());

        // ===== 偏好权重联动对比 =====
        List<PlanComparison> comparisons = null;
        if (req.getPrevCommuteWeight() != null && req.getPrevGaokaoOutputWeight() != null) {
            comparisons = new ArrayList<>();
            comparisons.add(compare(BATCH_QUOTA, quotaPool, QUOTA_CAP, gw,
                    req.getPrevCommuteWeight(), req.getPrevGaokaoOutputWeight(),
                    req.getCommuteWeight(), req.getGaokaoOutputWeight()));
            comparisons.add(compare(BATCH_TONGZHAO, tongzhaoPool, TONGZHAO_CAP, gw,
                    req.getPrevCommuteWeight(), req.getPrevGaokaoOutputWeight(),
                    req.getCommuteWeight(), req.getGaokaoOutputWeight()));
            comparisons.add(compare(BATCH_GUANTONG, guantongPool, GUANTONG_CAP, gw,
                    req.getPrevCommuteWeight(), req.getPrevGaokaoOutputWeight(),
                    req.getCommuteWeight(), req.getGaokaoOutputWeight()));
        }

        return GenerateResponse.builder()
                .studentId(s.getId()).studentName(s.getName()).totalScore(score)
                .juniorSchoolId(s.getJuniorSchoolId()).juniorZone(jsZone)
                .comprehensiveEval(compEval).quotaEligible(!quotaLocked && score >= controlLine)
                .issues(issues)
                .quotaPlan(quotaPlan).tongzhaoPlan(tongzhaoPlan).guantongPlan(guantongPlan)
                .guantongHidden(guantongHidden)
                .filteredByCommute(filteredByCommute).filteredByGaokaoTier(filteredByGaokao)
                .comparisons(comparisons)
                .build();
    }

    // ============ 候选校内部模型 ============
    private static class Candidate {
        final ReferenceData.HighSchoolView h;
        final int refScore;
        final int delta;
        final int commute;
        final int zoneDist;
        final GaokaoTier gaokaoTier;
        final String band;     // SPRINT/STEADY/SAFETY/NONE
        final boolean isGuantong;

        Candidate(ReferenceData.HighSchoolView h, int refScore, int delta, int commute,
                  int zoneDist, GaokaoTier gaokaoTier, String band, boolean isGuantong) {
            this.h = h; this.refScore = refScore; this.delta = delta; this.commute = commute;
            this.zoneDist = zoneDist; this.gaokaoTier = gaokaoTier; this.band = band; this.isGuantong = isGuantong;
        }
    }

    private static class GradientWeights {
        final int sprint, steady, safety;
        GradientWeights(int sprint, int steady, int safety) { this.sprint = sprint; this.steady = steady; this.safety = safety; }
    }

    /** 按分数差划分梯度区间（清单：冲刺+5~15 / 兜底−10~20，稳妥居间） */
    private static String bandOf(int delta) {
        if (delta >= SPRINT_DELTA_LOW && delta <= SPRINT_DELTA_HIGH) return "SPRINT";
        if (delta <= -SAFETY_DELTA_LOW && delta >= -SAFETY_DELTA_HIGH) return "SAFETY";
        if (delta > -SAFETY_DELTA_LOW && delta < SPRINT_DELTA_LOW) return "STEADY"; // (-10, 5)
        return "NONE";
    }

    /** 偏好加权排序分：通勤越近、高考出口梯队越高越靠前 */
    private int preferenceScore(Candidate c, int commuteW, int gaokaoW) {
        double commuteNorm = clamp(1.0 - (double) c.commute / MAX_COMMUTE, 0, 1) * 100;
        int gaokaoNorm = switch (c.gaokaoTier) {
            case TOP -> 100; case HEAD -> 75; case MID -> 50;
        };
        return (int) Math.round(commuteW * commuteNorm + gaokaoW * gaokaoNorm);
    }

    /** 按梯度权重分配容量，分档取偏好分最高的学校，组装成有序方案 */
    private List<GeneratedChoice> buildPlan(List<Candidate> pool, String batch, int cap,
                                            GradientWeights gw, int commuteW, int gaokaoW) {
        if (pool.isEmpty() || cap <= 0) return List.of();
        int nSprint = cap * gw.sprint / 100;
        int nSteady = cap * gw.steady / 100;
        int nSafety = cap - nSprint - nSteady; // 余量归入兜底

        Comparator<Candidate> byPref = Comparator.comparingInt((Candidate c) -> preferenceScore(c, commuteW, gaokaoW)).reversed();
        List<Candidate> sprint = top(pool, "SPRINT", byPref, nSprint);
        List<Candidate> steady = top(pool, "STEADY", byPref, nSteady);
        List<Candidate> safety = top(pool, "SAFETY", byPref, nSafety);
        List<Candidate> picked = new ArrayList<>(sprint);
        picked.addAll(steady);
        picked.addAll(safety);
        // 容量未满时，从「尚未入选」的候选校中按偏好分补足（先区间内的，再区间外的），避免方案过空
        if (picked.size() < cap) {
            Set<Candidate> chosen = new LinkedHashSet<>(picked);
            pool.stream().filter(c -> !chosen.contains(c))
                    .sorted(byPref)
                    .forEach(c -> {
                        if (picked.size() < cap) picked.add(c);
                    });
        }
        List<GeneratedChoice> out = new ArrayList<>();
        int p = 1;
        for (Candidate c : picked) {
            out.add(GeneratedChoice.builder()
                    .highSchoolId(c.h.id()).highSchoolName(null)
                    .batch(batch).tier(c.h.tier()).gaokaoTier(c.gaokaoTier.name())
                    .referenceScore(c.refScore).scoreBand(c.band).delta(c.delta)
                    .commuteMinutes(c.commute).zoneDistance(c.zoneDist)
                    .preferenceScore(preferenceScore(c, commuteW, gaokaoW))
                    .build());
            p++;
        }
        return out;
    }

    private List<Candidate> top(List<Candidate> pool, String band, Comparator<Candidate> byPref, int n) {
        if (n <= 0) return List.of();
        return pool.stream().filter(c -> band.equals(c.band))
                .sorted(byPref).limit(n).collect(Collectors.toList());
    }

    /** 偏好权重联动对比：用旧权重重排并与新权重结果比对 */
    private PlanComparison compare(String batch, List<Candidate> pool, int cap, GradientWeights gw,
                                   int prevCommuteW, int prevGaokaoW, int curCommuteW, int curGaokaoW) {
        List<Long> before = buildPlan(pool, batch, cap, gw, prevCommuteW, prevGaokaoW).stream()
                .map(GeneratedChoice::getHighSchoolId).collect(Collectors.toList());
        List<Long> after = buildPlan(pool, batch, cap, gw, curCommuteW, curGaokaoW).stream()
                .map(GeneratedChoice::getHighSchoolId).collect(Collectors.toList());
        Set<Long> beforeSet = new LinkedHashSet<>(before);
        Set<Long> afterSet = new LinkedHashSet<>(after);
        List<Long> added = after.stream().filter(id -> !beforeSet.contains(id)).collect(Collectors.toList());
        List<Long> removed = before.stream().filter(id -> !afterSet.contains(id)).collect(Collectors.toList());
        List<Long> reordered = after.stream()
                .filter(id -> beforeSet.contains(id) && after.indexOf(id) != before.indexOf(id))
                .collect(Collectors.toList());
        return PlanComparison.builder().batch(batch).before(before).after(after)
                .added(added).removed(removed).reordered(reordered).build();
    }

    // ============ 工具 ============
    private void capWarn(List<ValidationIssue> issues, String batch, int candidateCount, int cap) {
        if (candidateCount > cap) {
            issues.add(ValidationIssue.builder()
                    .code("CAPACITY").level("WARN").batch(batch)
                    .message("志愿数已达本批次上限（" + cap + "），无法新增，已截断至上限")
                    .build());
        }
    }

    private boolean evalMeetsB(String eval) {
        if (eval == null) return false;
        // 等级序：A > B > C > D；B 及以上达标
        return eval.equalsIgnoreCase("A") || eval.equalsIgnoreCase("B");
    }

    private boolean inRange(int w) { return w >= 0 && w <= 100; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** 缺 2025 线学校的参考分估算（与 ApplicationSimulator.estimateLine 一致） */
    private int estimateLine(ReferenceData.HighSchoolView h, int controlLine) {
        int tierBoost = "KEY".equals(h.tier()) ? 40 : 0;
        int compBoost = Math.max(0, (120 - h.tongzhaoQuota())) / 4;
        return controlLine + tierBoost + compBoost;
    }
}
