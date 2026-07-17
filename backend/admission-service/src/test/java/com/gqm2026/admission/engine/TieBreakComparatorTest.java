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
}
