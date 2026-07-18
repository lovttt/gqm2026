package com.gqm2026.school.entity;

/**
 * 高中校固有「高考出口梯队」（G7-Q2）。
 * 用于志愿生成器按用户「高考出口梯队偏好」(TOP/HEAD/MID) 匹配高中。
 * 种子数据由 HighSchool.tier 派生（KEY→TOP，NORMAL→MID），可在高中管理页编辑。
 */
public enum GaokaoTier {
    TOP, HEAD, MID
}
