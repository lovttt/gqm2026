package com.gqm2026.student.service;

import com.gqm2026.student.entity.Student;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * 校内排名比较器（口径同 03 §3.4 的北京中考同分比较链）。
 * 方向：成绩越高越靠前（降序），最后以准考证号自然序兜底，保证全序、确定性。
 * 比较顺序：
 *   1. 总分
 *   2. 语文+数学+外语 三科总分
 *   3. 语文  4. 数学  5. 外语
 *   6. 物理+道法 两科总分
 *   7. 物理  8. 道法  9. 体育
 *   10. ticketNo（自然序兜底）
 * 用于「校额到校资格：按初中校名额总数取前 N 名」（02 §2.6）。
 */
@Component
public class StudentTieBreakComparator {

    public Comparator<Student> comparator() {
        return Comparator
                .comparingInt(Student::getTotalScore).reversed()
                .thenComparing(Comparator.comparingInt(
                        (Student s) -> s.getChinese() + s.getMath() + s.getEnglish()).reversed())
                .thenComparing(Comparator.comparingInt(Student::getChinese).reversed())
                .thenComparing(Comparator.comparingInt(Student::getMath).reversed())
                .thenComparing(Comparator.comparingInt(Student::getEnglish).reversed())
                .thenComparing(Comparator.comparingInt(
                        (Student s) -> s.getPhysics() + s.getPolitics()).reversed())
                .thenComparing(Comparator.comparingInt(Student::getPhysics).reversed())
                .thenComparing(Comparator.comparingInt(Student::getPolitics).reversed())
                .thenComparing(Comparator.comparingInt(Student::getPe).reversed())
                .thenComparing(Comparator.comparing(Student::getTicketNo,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
