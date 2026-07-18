package com.gqm2026.admission.engine;

import com.gqm2026.admission.dto.StudentSnapshot;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 录取同分排序比较器（可配置）。
 * 排序方向：数值越大越优先（即“成绩更好”排前面）。
 * 比较顺序（前 9 级为成绩，第 10 级为确定性兜底），与 03 §3.4 一致：
 *   1. 总分 totalScore（满分 510）
 *   2. 语数外三科总分 chinese+math+english（满分 300）
 *   3. 语文 chinese（满分 100）
 *   4. 数学 math（满分 100）
 *   5. 英语 english（满分 100）
 *   6. 物理+道法两科总分 physics+politics（满分 160）
 *   7. 物理 physics（满分 80）
 *   8. 道法 politics（满分 80）
 *   9. 体育 pe（满分 50）
 *   10. 准考证号 ticketNo（自然序，作确定性兜底，确保全序）
 * 规则：前一级相同者，按下一级成绩高低依次排序；末级（ticketNo）仅作兜底，不纳入综合素质评价。
 * 如需调整成绩规则，只需修改下方 STEP 列表即可；第 10 级兜底不可删。
 */
@Component
public class TieBreakComparator {

    private static final List<java.util.function.Function<StudentSnapshot.StudentInfo, Integer>> STEP = List.of(
            s -> s.totalScore,                     // 1. 总分
            s -> s.chinese + s.math + s.english,   // 2. 语数外三科总分
            s -> s.chinese,                        // 3. 语文
            s -> s.math,                           // 4. 数学
            s -> s.english,                        // 5. 英语
            s -> s.physics + s.politics,           // 6. 物理+道法两科总分
            s -> s.physics,                        // 7. 物理
            s -> s.politics,                       // 8. 道法
            s -> s.pe                              // 9. 体育
    );

    public Comparator<StudentSnapshot.StudentInfo> comparator() {
        // 数值越大越优先（成绩更好排前面）：首级以 reverseOrder 降序，后续各级同样以 reverseOrder，
        // 避免对整条 thenComparing 链反复 reversed 导致首级排序被翻转为升序。
        Comparator<StudentSnapshot.StudentInfo> c = Comparator.comparing(STEP.get(0), Comparator.reverseOrder());
        for (int i = 1; i < STEP.size(); i++) {
            final int idx = i;
            c = c.thenComparing(STEP.get(idx), Comparator.reverseOrder());
        }
        // 第 10 级：准考证号自然序兜底，保证比较器为全序（任意两考生必有确定先后）
        c = c.thenComparing(s -> s.ticketNo);
        return c;
    }
}
