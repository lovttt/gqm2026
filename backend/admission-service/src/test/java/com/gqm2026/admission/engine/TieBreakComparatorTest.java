package com.gqm2026.admission.engine;

import com.gqm2026.admission.dto.StudentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TieBreakComparatorTest {

    private final TieBreakComparator tieBreak = new TieBreakComparator();

    private StudentSnapshot.StudentInfo info(String ticket, int total, int chinese, int math,
                                             int english, int physics, int politics, int pe) {
        return new StudentSnapshot.StudentInfo(1L, "n", ticket, 1L,
                chinese, math, english, physics, politics, pe, total, true);
    }

    @Test
    void higherTotalScoreAlwaysWinsRegardlessOfTicketNo() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        var hi = info("9999", 500, 0, 0, 0, 0, 0, 0);   // 高分、准考证号靠后
        var lo = info("0001", 400, 100, 100, 100, 80, 80, 50); // 低分、准考证号靠前
        assertTrue(c.compare(hi, lo) < 0, "高分者应排在前（compare<0）");
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(lo, hi));
        list.sort(c);
        assertSame(hi, list.get(0));
    }

    @Test
    void ticketNoIsDeterministicFinalTiebreak() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        // 9 级成绩完全相同，仅准考证号不同
        var a = info("1002", 480, 90, 90, 90, 80, 80, 50);
        var b = info("1001", 480, 90, 90, 90, 80, 80, 50);
        // 比较器为全序：任意两考生必有确定先后
        assertNotEquals(0, c.compare(a, b));
        // 准考证号自然序（升序）兜底：1001 在前
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(a, b));
        list.sort(c);
        assertEquals("1001", list.get(0).ticketNo);
        assertEquals("1002", list.get(1).ticketNo);
    }

    @Test
    void sameTicketNoAndSameScoresAreEqual() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        var a = info("1001", 480, 90, 90, 90, 80, 80, 50);
        var b = info("1001", 480, 90, 90, 90, 80, 80, 50);
        assertEquals(0, c.compare(a, b));
    }

    // ===== 03 §3.4 新增级别（2/6/7）专项断言 =====

    @Test
    void level2ThreeSubjectSumBreaksTotalTie() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        // 总分相等（均 470），语数外三科总分不同：A=270(90+90+90) vs B=260(100+80+80)，
        // 用体育补偿使总分相等：A pe=40，B pe=50，物理/道法均为 80。
        var a = info("2001", 470, 90, 90, 90, 80, 80, 40);
        var b = info("2002", 470, 100, 80, 80, 80, 80, 50);
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(b, a));
        list.sort(c);
        assertEquals("2001", list.get(0).ticketNo, "语数外三科总分更高者优先");
    }

    @Test
    void level6PhysicsPoliticsSumBreaksTie() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        // 总分相等（语数外均为 90，物理+道法不同）：A=160(80+80) vs B=140(70+70)，
        // 用体育补偿：A pe=30，B pe=50。
        var a = info("3001", 470, 90, 90, 90, 80, 80, 30);
        var b = info("3002", 470, 90, 90, 90, 70, 70, 50);
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(b, a));
        list.sort(c);
        assertEquals("3001", list.get(0).ticketNo, "物理+道法两科总分更高者优先");
    }

    @Test
    void level7PhysicsBreaksTieWhenComboSumEqual() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        // 物理+道法两科总分相等（均 140），但物理不同：A=80+60 vs B=70+70，
        // 体育相同(50)，总分相等。
        var a = info("4001", 460, 90, 90, 90, 80, 60, 50);
        var b = info("4002", 460, 90, 90, 90, 70, 70, 50);
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(b, a));
        list.sort(c);
        assertEquals("4001", list.get(0).ticketNo, "两科总分相同时物理更高者优先");
    }

    @Test
    void fullChainIsDeterministicTotalOrder() {
        Comparator<StudentSnapshot.StudentInfo> c = tieBreak.comparator();
        // 全部 9 级成绩相同、仅准考证号不同 → 必然由 ticketNo 决定全序
        var s1 = info("5003", 480, 90, 90, 90, 80, 80, 50);
        var s2 = info("5001", 480, 90, 90, 90, 80, 80, 50);
        var s3 = info("5002", 480, 90, 90, 90, 80, 80, 50);
        List<StudentSnapshot.StudentInfo> list = new ArrayList<>(List.of(s1, s2, s3));
        list.sort(c);
        assertArrayEquals(new String[]{"5001", "5002", "5003"},
                list.stream().map(x -> x.ticketNo).toArray());
    }
}
