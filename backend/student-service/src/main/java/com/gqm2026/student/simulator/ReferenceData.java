package com.gqm2026.student.simulator;

import java.util.*;
import java.util.stream.Collectors;

/** 志愿模拟所需的参考数据（与 REST DTO 解耦，便于单测直接构造） */
public class ReferenceData {

    /** 高中视图：含层次与片区，用于「学校层次偏好」「区域/离家距离」因子 */
    public record HighSchoolView(Long id, String tier, int zone, int tongzhaoQuota) {}

    public final List<HighSchoolView> highSchools;
    public final Map<Long, HighSchoolView> hsById;
    /** 初中校 id -> 片区 */
    public final Map<Long, Integer> juniorZoneById;
    /** 初中校 id -> 其对口且有名额的高中 id 列表 */
    public final Map<Long, List<Long>> quotaHighSchoolsByJs;
    /** 全区最低控制线（校额到校），同时作为统招普高线使用 */
    public final int controlLine;
    /** 高中 id -> 2025 统招录取线（用于志愿模拟「冲/稳/保」分档） */
    public final Map<Long, Integer> line2025ById;
    /** 分数 -> 区排名(累计人数)：2025 用于学校录取排名，2026 用于考生排名 */
    public final Map<Integer, Integer> rank2025ByScore;
    public final Map<Integer, Integer> rank2026ByScore;

    public ReferenceData(List<HighSchoolView> highSchools,
                         Map<Long, HighSchoolView> hsById,
                         Map<Long, Integer> juniorZoneById,
                         Map<Long, List<Long>> quotaHighSchoolsByJs,
                         int controlLine,
                         Map<Long, Integer> line2025ById,
                         Map<Integer, Integer> rank2025ByScore,
                         Map<Integer, Integer> rank2026ByScore) {
        this.highSchools = highSchools;
        this.hsById = hsById;
        this.juniorZoneById = juniorZoneById;
        this.quotaHighSchoolsByJs = quotaHighSchoolsByJs;
        this.controlLine = controlLine;
        this.line2025ById = line2025ById;
        this.rank2025ByScore = rank2025ByScore;
        this.rank2026ByScore = rank2026ByScore;
    }

    /** 把某年某分数换算成区排名（累计人数，即 ≥ 该分数的人数）。 */
    public int cumulativeRank(int year, int score) {
        Map<Integer, Integer> m = (year == 2025) ? rank2025ByScore : rank2026ByScore;
        if (score > 500) score = 500;
        Integer exact = m.get(score);
        if (exact != null) return exact;
        // 分段表缺该分数时，取「不高于该分数的最近一个已记录分数」的累计人数近似
        int bestScore = -1;
        int bestCum = m.isEmpty() ? 0 : m.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (Map.Entry<Integer, Integer> e : m.entrySet()) {
            if (e.getKey() <= score && e.getKey() > bestScore) {
                bestScore = e.getKey();
                bestCum = e.getValue();
            }
        }
        return bestCum;
    }

    public static ReferenceData from(SchoolDataset sd) {
        List<SchoolDataset.HighSchoolInfo> hsList =
                sd.highSchools == null ? List.of() : sd.highSchools;
        List<HighSchoolView> hs = hsList.stream()
                .map(h -> new HighSchoolView(h.id, h.tier, h.zone, h.tongzhaoQuota))
                .toList();
        Map<Long, HighSchoolView> hsById = hs.stream()
                .collect(Collectors.toMap(HighSchoolView::id, h -> h));

        Map<Long, Integer> jsZone = (sd.juniorSchools == null ? List.<SchoolDataset.JuniorSchoolInfo>of() : sd.juniorSchools)
                .stream().collect(Collectors.toMap(j -> j.id, j -> j.zone));

        Map<Long, List<Long>> quotaByJs = new HashMap<>();
        if (sd.quotaSeats != null) {
            for (SchoolDataset.QuotaSeatInfo q : sd.quotaSeats) {
                if (q.quota > 0) {
                    quotaByJs.computeIfAbsent(q.juniorSchoolId, k -> new ArrayList<>()).add(q.highSchoolId);
                }
            }
        }

        // 共享名额池：把每组内所有成员的对口高中合并后，赋给组内每一所学校。
        // 这样东直门（无名额行）的学生也能以 165 的对口高中作为校额到校志愿选项，
        // 录取时再在 AdmissionEngine 的合并名额池中竞争。
        if (sd.quotaGroups != null) {
            for (List<Long> g : sd.quotaGroups) {
                if (g == null || g.isEmpty()) continue;
                Set<Long> union = new LinkedHashSet<>();
                for (Long member : g) {
                    if (quotaByJs.containsKey(member)) union.addAll(quotaByJs.get(member));
                }
                for (Long member : g) {
                    quotaByJs.put(member, new ArrayList<>(union));
                }
            }
        }

        Map<Long, Integer> line2025 = new HashMap<>();
        if (sd.scoreLines != null) {
            for (SchoolDataset.ScoreLineInfo sl : sd.scoreLines) {
                if (sl.year == 2025 && "TONGZHAO".equals(sl.batch)) {
                    line2025.put(sl.highSchoolId, sl.score);
                }
            }
        }

        Map<Integer, Integer> rank2025 = new HashMap<>();
        Map<Integer, Integer> rank2026 = new HashMap<>();
        if (sd.scoreSegments != null) {
            for (SchoolDataset.ScoreSegmentInfo seg : sd.scoreSegments) {
                if (seg.cumulative == null) continue;
                if (seg.year == 2025) rank2025.put(seg.score, seg.cumulative);
                else if (seg.year == 2026) rank2026.put(seg.score, seg.cumulative);
            }
        }

        int cl = (sd.controlLine != null) ? sd.controlLine.value : 0;
        return new ReferenceData(hs, hsById, jsZone, quotaByJs, cl, line2025, rank2025, rank2026);
    }
}
