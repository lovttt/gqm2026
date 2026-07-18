package com.gqm2026.admission.entity;

/**
 * 录取状态值对象（充血）：已录取 / 未录取。
 */
public enum AdmissionStatus {
    ADMITTED,       // 已录取
    NOT_ADMITTED;   // 未录取（滑档）

    public static AdmissionStatus from(String s) {
        if (s == null || s.isBlank()) return null;
        return valueOf(s.trim().toUpperCase());
    }
}
