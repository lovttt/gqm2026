package com.gqm2026.admission.engine;

import com.gqm2026.admission.dto.StudentSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 录取同分排序比较器（可配置）。
 * 排序方向：数值越大越优先（即“成绩更好”排前面）。
 * 比较顺序（前 4 级为成绩，第 5 级为确定性兜底）：
 *   1. 总分（由高到低）
 *   2. 语文（相同则比较下一项）
 *   3. 数学
 *   4. 英语
 *   5. 准考证号 ticketNo（自然序，作确定性兜底，确保全序）
 * 规则：总分相同者，按「语文 → 数学 → 英语」的单科分数高低依次排序。
 * 如需调整成绩规则，只需修改下方 STEP 列表即可；第 5 级兜底不可删。
 */
@Component
public class TieBreakComparator {

    private static final List<java.util.function.ToIntFunction<StudentSnapshot.StudentInfo>> STEP = List.of(
            s -> s.totalScore,
            s -> s.chinese,
            s -> s.math,
            s -> s.english
    );

    public Comparator<StudentSnapshot.StudentInfo> comparator() {
        Comparator<StudentSnapshot.StudentInfo> c = Comparator.comparingInt(STEP.get(0)).reversed();
        for (int i = 1; i < STEP.size(); i++) {
            final int idx = i;
            c = c.thenComparingInt(STEP.get(idx)).reversed();
        }
        // 第 5 级：准考证号自然序兜底，保证比较器为全序（任意两考生必有确定先后）
        c = c.thenComparing(s -> s.ticketNo);
        return c;
    }
}
