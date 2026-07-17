package com.gqm2026.school.config;

import java.util.List;

/**
 * 共享校额名额的初中校分组（单一事实来源）。
 * 同一组内的名额合并计算（按高中汇总），查询展示时两个学校都列出。
 * 例：东直门与165一体 → 两校共用一个名额池，查东直门（或165）都返回合并后的名额。
 */
public final class QuotaGroupConfig {

    /** 每组用学校全名标识，导出时再转换为数据库 id 分组 */
    public static final List<List<String>> JUNIOR_GROUPS = List.of(
            List.of("北京市东直门中学", "北京市第一六五中学"),
            List.of("北京市广渠门中学", "北京市龙潭中学"),
            List.of("北京市第一中学", "北京市第五中学分校"),
            List.of("北京汇文实验中学", "北京汇文中学"),
            List.of("北京市第二十一中学", "北京市第二十二中学")
    );

    private QuotaGroupConfig() {
    }
}
