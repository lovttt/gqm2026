package com.gqm2026.admission.entity;

/**
 * 录取批次值对象（充血）：校额到校 / 统招。
 * 作为领域语言的一部分，应用/接口层以枚举表达批次，避免散落的字符串字面量。
 */
public enum Batch {
    QUOTA,      // 校额到校
    TONGZHAO;   // 统招

    public static Batch from(String s) {
        if (s == null || s.isBlank()) return null;
        return valueOf(s.trim().toUpperCase());
    }
}
