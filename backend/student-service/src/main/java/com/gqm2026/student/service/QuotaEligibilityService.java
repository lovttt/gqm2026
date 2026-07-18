package com.gqm2026.student.service;

import com.gqm2026.student.entity.Student;
import com.gqm2026.student.repository.StudentRepository;
import com.gqm2026.student.simulator.SchoolDataFetcher;
import com.gqm2026.student.simulator.SchoolDataset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 校额到校资格重算（结合初中校名额总数，见 docs/spec/02 §2.6）。
 *
 * 规则：某初中校被分配的校额名额总数 N = Σ QuotaSeat.quota（该校对所有高中之和）。
 * 对同一初中校内「总分 ≥ 控制线(430)」的考生按 {@link StudentTieBreakComparator} 降序排名，
 * 前 N 名 hasQuotaEligibility=true，其余（含超出前 N、低于控制线者）为 false。
 * 达线人数不足 N 时，达线者全部具资格（资格数 = min(N, 达线人数)）；N=0 则该校无人具资格。
 */
@Service
@RequiredArgsConstructor
public class QuotaEligibilityService {

    private final SchoolDataFetcher schoolDataFetcher;
    private final StudentRepository studentRepository;
    private final StudentTieBreakComparator tieBreak;

    /** 结合各初中校名额总数重算全部考生的校额资格，返回按校统计。 */
    @Transactional
    public Map<String, Object> recompute() {
        SchoolDataset sd = schoolDataFetcher.fetchRaw();
        int controlLine = (sd != null && sd.controlLine != null) ? sd.controlLine.value : 0;

        // 初中校 -> 名额总数 N（Σ QuotaSeat.quota）
        Map<Long, Integer> quotaTotalByJs = new HashMap<>();
        if (sd != null && sd.quotaSeats != null) {
            for (SchoolDataset.QuotaSeatInfo q : sd.quotaSeats) {
                quotaTotalByJs.merge(q.juniorSchoolId, q.quota, Integer::sum);
            }
        }

        List<Student> students = studentRepository.findAll();
        Map<Long, List<Student>> byJs = students.stream()
                .collect(Collectors.groupingBy(Student::getJuniorSchoolId));

        Comparator<Student> cmp = tieBreak.comparator();
        List<Map<String, Object>> byJunior = new ArrayList<>();
        int eligibleTotal = 0;

        for (Map.Entry<Long, List<Student>> e : byJs.entrySet()) {
            Long jsId = e.getKey();
            List<Student> group = e.getValue();
            int n = quotaTotalByJs.getOrDefault(jsId, 0);

            // 先全部置无资格
            for (Student s : group) s.setHasQuotaEligibility(false);

            // 达线者按 comparator 降序排，取前 N 名（N=该校名额总数）具资格；
            // 资格数 = min(达线人数, N)，超出前 N 名或未达线者一律无资格（对应 02 §2.6）
            List<Student> passing = group.stream()
                    .filter(s -> s.getTotalScore() >= controlLine)
                    .sorted(cmp)
                    .collect(Collectors.toList());
            int eligibleHere = Math.min(passing.size(), n);
            for (int i = 0; i < eligibleHere; i++) {
                passing.get(i).setHasQuotaEligibility(true);
            }
            eligibleTotal += eligibleHere;

            Map<String, Object> stat = new HashMap<>();
            stat.put("juniorSchoolId", jsId);
            stat.put("quotaTotal", n);
            stat.put("eligible", eligibleHere);
            byJunior.add(stat);
        }

        // 分块持久化，避免超长事务写锁争用
        int chunk = 1000;
        for (int i = 0; i < students.size(); i += chunk) {
            studentRepository.saveAll(students.subList(i, Math.min(students.size(), i + chunk)));
        }

        byJunior.sort(Comparator.comparingLong(m -> (Long) m.get("juniorSchoolId")));
        Map<String, Object> result = new HashMap<>();
        result.put("eligibleTotal", eligibleTotal);
        result.put("studentTotal", students.size());
        result.put("byJunior", byJunior);
        return result;
    }
}
