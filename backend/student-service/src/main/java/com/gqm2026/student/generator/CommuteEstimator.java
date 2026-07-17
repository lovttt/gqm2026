package com.gqm2026.student.generator;

/**
 * 家校通勤时长估算（占位接口）。
 *
 * <p>用户清单要求「联动东城区实时交通数据」，但当前系统无交通 API / 实时路况数据源。
 * 按决策采用「片区(zone)距离近似」：以初中校片区与高中片区的差估算通勤分钟数。
 * 后续若接入真实交通 API，仅需替换 {@link #estimateMinutes} 实现，调用方无需改动。
 */
public interface CommuteEstimator {

    /**
     * 估算从某初中校到某高中的通勤时长（分钟）。
     *
     * @param juniorSchoolId 初中校 id（预留真实路网/API 用）
     * @param juniorZone     初中校片区
     * @param highSchoolId   高中 id（预留真实路网/API 用）
     * @param highZone       高中片区
     */
    int estimateMinutes(long juniorSchoolId, int juniorZone, long highSchoolId, int highZone);
}
