package com.gqm2026.admission.engine;

import com.gqm2026.admission.dto.SchoolSnapshot;
import com.gqm2026.admission.dto.StudentSnapshot;
import com.gqm2026.admission.entity.AdmissionResult;
import com.gqm2026.admission.entity.AdmissionStatus;
import com.gqm2026.admission.repository.AdmissionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 录取领域服务（充血）：校额到校 + 统招两个批次的录取计算与结果落库。
 *
 * <p>设计约束（G7 / 09 §2.5 / §3.2）：
 * <ul>
 *   <li>跨服务 HTTP 拉取（school/student 快照）已移出本服务，由 {@code AdmissionAppService} 经
 *       {@code SchoolSnapshotPort}/{@code StudentSnapshotPort} 在事务外拉取后传入，本服务只做纯计算 + 落库；</li>
 *   <li>{@code runXxx} 标注 {@code @Transactional}，一个用例一个事务；</li>
 *   <li>批次 / 状态判定统一经 {@link com.gqm2026.admission.entity.Batch}/{@link AdmissionStatus} 表达。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdmissionEngine {

    private final AdmissionResultRepository resultRepo;
    private final TieBreakComparator tieBreak;

    /** 一次模拟运行的批次号：在最新 runId 基础上 +1，保证多次运行互不覆盖（历史快照） */
    private Long nextRunId() {
        Long latest = resultRepo.findLatestRunId();
        return (latest == null ? 0L : latest) + 1;
    }

    /** 一键顺序模拟：校额到校 -> 统招。每次生成新的 runId，结果追加而非清空。 */
    @Transactional
    public Map<String, Object> runFull(SchoolSnapshot ss, StudentSnapshot st) {
        Long runId = nextRunId();
        LocalDateTime runAt = LocalDateTime.now();
        Set<Long> quotaAdmitted = runQuota(ss, st, runId, runAt);
        runTongzhao(ss, st, quotaAdmitted, runId, runAt);
        return stats(runId);
    }

    @Transactional
    public Map<String, Object> runQuotaOnly(SchoolSnapshot ss, StudentSnapshot st) {
        Long runId = nextRunId();
        LocalDateTime runAt = LocalDateTime.now();
        runQuota(ss, st, runId, runAt);
        return stats(runId);
    }

    @Transactional
    public Map<String, Object> runTongzhaoOnly(SchoolSnapshot ss, StudentSnapshot st) {
        Long runId = nextRunId();
        LocalDateTime runAt = LocalDateTime.now();
        // 豁免集取「最新一次运行」的校额到校已录取考生，避免跨运行污染
        Long latest = resultRepo.findLatestRunId();
        Set<Long> exempt = (latest == null) ? Set.of() :
                resultRepo.findByRunId(latest).stream()
                        .filter(r -> "QUOTA".equals(r.getBatch()))
                        .map(AdmissionResult::getStudentId)
                        .collect(Collectors.toSet());
        runTongzhao(ss, st, exempt, runId, runAt);
        return stats(runId);
    }

    /**
     * 校额到校批次：先于统招。
     * 按「共享名额池（组）」聚合：同一组（如 一中/五中分）的所有考生放在一起，按分数从高到低排名；
     * 每位考生按自己填报的校额到校志愿顺序（priority 1..10）依次尝试，第一个仍有余额的高中即被录取——
     * 即「分数优先、遵循志愿」。组内某高中名额用尽，则该考生顺延到下一志愿；所有志愿均无余额则进入统招。
     */
    private Set<Long> runQuota(SchoolSnapshot ss, StudentSnapshot st, Long runId, LocalDateTime runAt) {
        int controlLine = ss.controlLine != null ? ss.controlLine.value : 0;

        Map<Long, String> hsName = new HashMap<>();
        for (SchoolSnapshot.HighSchoolInfo h : ss.highSchools) hsName.put(h.id, h.name);
        Map<Long, String> jsName = new HashMap<>();
        for (SchoolSnapshot.JuniorSchoolInfo j : ss.juniorSchools) jsName.put(j.id, j.name);

        // 共享名额池：同一组内的初中校共用一个名额池（按高中汇总），组代表 id = 组内第一个 id
        Map<Long, Long> groupKeyOf = new HashMap<>();
        if (ss.quotaGroups != null) {
            for (List<Long> g : ss.quotaGroups) {
                if (g == null || g.isEmpty()) continue;
                Long canonical = g.get(0);
                for (Long id : g) groupKeyOf.put(id, canonical);
            }
        }
        // 每个 (组, 高中) 的校额名额
        Map<String, Integer> quotaMap = new HashMap<>();
        for (SchoolSnapshot.QuotaSeatInfo q : ss.quotaSeats) {
            Long gk = groupKeyOf.getOrDefault(q.juniorSchoolId, q.juniorSchoolId);
            quotaMap.merge(gk + "_" + q.highSchoolId, q.quota, Integer::sum);
        }

        // 考生校额志愿：按 studentId 分组并按 priority 升序（即志愿先后顺序）
        Map<Long, List<StudentSnapshot.ApplicationInfo>> quotaAppsByStudent = st.applications.stream()
                .filter(a -> "QUOTA".equals(a.batch))
                .collect(Collectors.groupingBy(a -> a.studentId));
        quotaAppsByStudent.values().forEach(list -> list.sort(Comparator.comparingInt(a -> a.priority)));

        // 按共享组聚合候选考生：具备资格 + 过控制线 + 至少填报 1 个校额志愿
        Map<Long, List<StudentSnapshot.StudentInfo>> byGroup = new HashMap<>();
        for (StudentSnapshot.StudentInfo s : st.students) {
            if (!s.hasQuotaEligibility) continue;
            if (s.totalScore < controlLine) continue;
            List<StudentSnapshot.ApplicationInfo> apps = quotaAppsByStudent.get(s.id);
            if (apps == null || apps.isEmpty()) continue;
            Long gk = groupKeyOf.getOrDefault(s.juniorSchoolId, s.juniorSchoolId);
            byGroup.computeIfAbsent(gk, k -> new ArrayList<>()).add(s);
        }

        Set<Long> admitted = new HashSet<>();
        Comparator<StudentSnapshot.StudentInfo> cmp = tieBreak.comparator();
        for (Map.Entry<Long, List<StudentSnapshot.StudentInfo>> e : byGroup.entrySet()) {
            Long gk = e.getKey();
            List<StudentSnapshot.StudentInfo> cands = e.getValue();
            cands.sort(cmp); // 按分数（含同分比较器）从高到低
            // 本组剩余名额（key 形如 gk_hsId）
            Map<String, Integer> remaining = new HashMap<>();
            String prefix = gk + "_";
            for (Map.Entry<String, Integer> qe : quotaMap.entrySet()) {
                if (qe.getKey().startsWith(prefix)) remaining.put(qe.getKey(), qe.getValue());
            }
            int rank = 0;
            for (StudentSnapshot.StudentInfo s : cands) {
                rank++; // 共享组内分数排名（1 起）
                List<StudentSnapshot.ApplicationInfo> apps = quotaAppsByStudent.get(s.id);
                if (apps == null) continue;
                // 按志愿顺序挑第一个仍有余额的高中
                for (StudentSnapshot.ApplicationInfo a : apps) {
                    String key = gk + "_" + a.highSchoolId;
                    if (remaining.getOrDefault(key, 0) > 0) {
                        remaining.put(key, remaining.get(key) - 1);
                        admitted.add(s.id);
                        Long hsId = a.highSchoolId;
                        resultRepo.save(AdmissionResult.builder()
                                .studentId(s.id).studentName(s.name).ticketNo(s.ticketNo)
                                .batch("QUOTA").highSchoolId(hsId).highSchoolName(hsName.get(hsId))
                                .juniorSchoolId(s.juniorSchoolId).juniorSchoolName(jsName.get(s.juniorSchoolId))
                                .totalScore(s.totalScore).chinese(s.chinese).math(s.math).english(s.english)
                                .physics(s.physics).politics(s.politics).pe(s.pe)
                                .status("ADMITTED")
                                .note("校额到校第" + a.priority + "志愿录取（共享组内分数排名第" + rank + "）")
                                .schoolRank(rank)
                                .runId(runId).runAt(runAt).createdAt(LocalDateTime.now()).build());
                        break;
                    }
                }
            }
        }
        return admitted;
    }

    /**
     * 统招批次：平行志愿（分数优先、遵循志愿、一次投档）。
     * 候选人按全局分数+同分比较器排序，每个学生仅在其分数档位投档一次；
     * 按其志愿优先级顺序取「仍有余额」的第一个学校即止——填在 2/3 志愿不会被罚到队尾。
     * 所有志愿学校均无余额 -> NOT_ADMITTED（无补录批次）。
     */
    private void runTongzhao(SchoolSnapshot ss, StudentSnapshot st, Set<Long> exempt, Long runId, LocalDateTime runAt) {
        Map<Long, Integer> remaining = new HashMap<>();
        Map<Long, String> hsName = new HashMap<>();
        for (SchoolSnapshot.HighSchoolInfo h : ss.highSchools) {
            remaining.put(h.id, h.tongzhaoQuota);
            hsName.put(h.id, h.name);
        }
        Map<Long, String> jsName = new HashMap<>();
        for (SchoolSnapshot.JuniorSchoolInfo j : ss.juniorSchools) jsName.put(j.id, j.name);

        List<StudentSnapshot.StudentInfo> candidates = st.students.stream()
                .filter(s -> !exempt.contains(s.id)).collect(Collectors.toList());
        candidates.sort(tieBreak.comparator());

        Map<Long, List<StudentSnapshot.ApplicationInfo>> appsMap = st.applications.stream()
                .filter(a -> "TONGZHAO".equals(a.batch))
                .collect(Collectors.groupingBy(a -> a.studentId));
        appsMap.values().forEach(list -> list.sort(Comparator.comparingInt(a -> a.priority)));

        for (StudentSnapshot.StudentInfo s : candidates) {
            List<StudentSnapshot.ApplicationInfo> apps = appsMap.getOrDefault(s.id, List.of());
            boolean admitted = false;
            for (StudentSnapshot.ApplicationInfo a : apps) {
                Long hs = a.highSchoolId;
                if (remaining.getOrDefault(hs, 0) > 0) {
                    remaining.put(hs, remaining.get(hs) - 1);
                    resultRepo.save(AdmissionResult.builder()
                            .studentId(s.id).studentName(s.name).ticketNo(s.ticketNo)
                            .batch("TONGZHAO").highSchoolId(hs).highSchoolName(hsName.get(hs))
                            .juniorSchoolId(s.juniorSchoolId).juniorSchoolName(jsName.get(s.juniorSchoolId))
                            .totalScore(s.totalScore).chinese(s.chinese).math(s.math).english(s.english)
                            .physics(s.physics).politics(s.politics).pe(s.pe)
                            .status("ADMITTED")
                            .note("统招第" + a.priority + "志愿录取")
                            .runId(runId).runAt(runAt).createdAt(LocalDateTime.now()).build());
                    admitted = true;
                    break;
                }
            }
            if (!admitted) {
                resultRepo.save(AdmissionResult.builder()
                        .studentId(s.id).studentName(s.name).ticketNo(s.ticketNo)
                        .batch("TONGZHAO").highSchoolId(null).highSchoolName(null)
                        .juniorSchoolId(s.juniorSchoolId).juniorSchoolName(jsName.get(s.juniorSchoolId))
                        .totalScore(s.totalScore).chinese(s.chinese).math(s.math).english(s.english)
                        .physics(s.physics).politics(s.politics).pe(s.pe)
                        .status("NOT_ADMITTED")
                        .note("统招未录取/滑档（平行志愿，未落入队尾惩罚）")
                        .runId(runId).runAt(runAt).createdAt(LocalDateTime.now()).build());
            }
        }
    }

    public Map<String, Object> currentStats() {
        Long latest = resultRepo.findLatestRunId();
        if (latest == null) return emptyStats();
        return stats(latest);
    }

    private Map<String, Object> stats(Long runId) {
        List<AdmissionResult> all = resultRepo.findByRunId(runId);
        long total = all.size();
        long admitted = all.stream().filter(r -> "ADMITTED".equals(r.getStatus())).count();
        long quotaAdmitted = all.stream()
                .filter(r -> "QUOTA".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus())).count();
        long tongzhaoAdmitted = all.stream()
                .filter(r -> "TONGZHAO".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus())).count();
        return Map.of(
                "runId", runId,
                "total", total,
                "admitted", admitted,
                "notAdmitted", total - admitted,
                "quotaAdmitted", quotaAdmitted,
                "tongzhaoAdmitted", tongzhaoAdmitted
        );
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("runId", null);
        m.put("total", 0L); m.put("admitted", 0L); m.put("notAdmitted", 0L);
        m.put("quotaAdmitted", 0L); m.put("tongzhaoAdmitted", 0L);
        return m;
    }

    /** 默认返回最新一次模拟运行的结果，支持按毕业学校 / 录取学校 / 分数范围 / 状态 / 姓名过滤 */
    public Page<AdmissionResult> results(Pageable pageable, Long juniorSchoolId, Long highSchoolId,
                                          Integer minScore, Integer maxScore, AdmissionStatus status, String studentName) {
        Long latest = resultRepo.findLatestRunId();
        if (latest == null) return Page.empty(pageable);
        Specification<AdmissionResult> spec = (root, q, cb) -> cb.equal(root.get("runId"), latest);
        if (juniorSchoolId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("juniorSchoolId"), juniorSchoolId));
        }
        if (highSchoolId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("highSchoolId"), highSchoolId));
        }
        if (minScore != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("totalScore"), minScore));
        }
        if (maxScore != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("totalScore"), maxScore));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status.name()));
        }
        if (studentName != null && !studentName.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.like(root.get("studentName"), "%" + studentName.trim() + "%"));
        }
        return resultRepo.findAll(spec, pageable);
    }

    public List<AdmissionResult> resultsByStudent(Long studentId) {
        Long latest = resultRepo.findLatestRunId();
        List<AdmissionResult> all = resultRepo.findByStudentId(studentId);
        if (latest == null) return List.of();
        return all.stream().filter(r -> latest.equals(r.getRunId())).collect(Collectors.toList());
    }

    /** 列出全部历史模拟运行（含每轮统计），用于多次模拟对比 */
    public List<Map<String, Object>> runs() {
        List<AdmissionResult> all = resultRepo.findAll();
        Map<Long, List<AdmissionResult>> byRun = all.stream()
                .collect(Collectors.groupingBy(AdmissionResult::getRunId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, List<AdmissionResult>> e : byRun.entrySet()) {
            Long runId = e.getKey();
            List<AdmissionResult> rs = e.getValue();
            long total = rs.size();
            long admitted = rs.stream().filter(r -> "ADMITTED".equals(r.getStatus())).count();
            long quotaAdmitted = rs.stream()
                    .filter(r -> "QUOTA".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus())).count();
            long tongzhaoAdmitted = rs.stream()
                    .filter(r -> "TONGZHAO".equals(r.getBatch()) && "ADMITTED".equals(r.getStatus())).count();
            LocalDateTime runAt = rs.stream().map(AdmissionResult::getRunAt)
                    .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
            Map<String, Object> entry = new HashMap<>();
            entry.put("runId", runId);
            entry.put("runAt", runAt);
            entry.put("total", total);
            entry.put("admitted", admitted);
            entry.put("notAdmitted", total - admitted);
            entry.put("quotaAdmitted", quotaAdmitted);
            entry.put("tongzhaoAdmitted", tongzhaoAdmitted);
            list.add(entry);
        }
        list.sort((a, b) -> Long.compare((Long) b.get("runId"), (Long) a.get("runId")));
        return list;
    }

    public List<AdmissionResult> resultsByRun(Long runId) {
        return resultRepo.findByRunId(runId);
    }

    /** 按高中聚合最近一次模拟运行的录取情况，用于「查看各校录取情况」 */
    public List<Map<String, Object>> summaryBySchool(SchoolSnapshot ss) {
        Long latest = resultRepo.findLatestRunId();
        if (latest == null) return List.of();
        List<AdmissionResult> results = resultRepo.findByRunId(latest);

        Map<Long, Integer> tongzhaoPlan = ss.highSchools.stream()
                .collect(Collectors.toMap(h -> h.id, h -> h.tongzhaoQuota));
        Map<Long, Integer> quotaPlan = ss.quotaSeats.stream()
                .collect(Collectors.groupingBy(q -> q.highSchoolId,
                        Collectors.summingInt(q -> q.quota)));

        Map<Long, List<AdmissionResult>> byHs = results.stream()
                .filter(r -> r.getHighSchoolId() != null && "ADMITTED".equals(r.getStatus()))
                .collect(Collectors.groupingBy(AdmissionResult::getHighSchoolId));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SchoolSnapshot.HighSchoolInfo h : ss.highSchools) {
            long hsId = h.id;
            List<AdmissionResult> admitted = byHs.getOrDefault(hsId, List.of());
            long quotaAdm = admitted.stream().filter(r -> "QUOTA".equals(r.getBatch())).count();
            long tongAdm = admitted.stream().filter(r -> "TONGZHAO".equals(r.getBatch())).count();
            int min = admitted.stream().mapToInt(AdmissionResult::getTotalScore).min().orElse(0);
            int max = admitted.stream().mapToInt(AdmissionResult::getTotalScore).max().orElse(0);
            int plan = tongzhaoPlan.getOrDefault(hsId, 0) + quotaPlan.getOrDefault(hsId, 0);
            long adm = quotaAdm + tongAdm;
            double fill = plan == 0 ? 0 : (double) adm / plan;
            Map<String, Object> m = new HashMap<>();
            m.put("highSchoolId", hsId);
            m.put("name", h.name);
            m.put("tongzhaoPlan", tongzhaoPlan.getOrDefault(hsId, 0));
            m.put("quotaPlan", quotaPlan.getOrDefault(hsId, 0));
            m.put("tongzhaoAdmitted", tongAdm);
            m.put("quotaAdmitted", quotaAdm);
            m.put("admitted", adm);
            m.put("minScore", admitted.isEmpty() ? null : min);
            m.put("maxScore", admitted.isEmpty() ? null : max);
            m.put("fillRate", Math.round(fill * 1000.0) / 10.0);
            list.add(m);
        }
        return list;
    }
}
