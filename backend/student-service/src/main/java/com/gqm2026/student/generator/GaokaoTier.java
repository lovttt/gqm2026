package com.gqm2026.student.generator;

/**
 * 高考出口梯队（占位枚举）。
 *
 * <p>用户清单要求按 TOP级 / 头部 / 中上游 三档匹配对应高考成绩梯队的学校。
 * 当前 {@code HighSchool} 仅有 {@code tier}(KEY/NORMAL) 两层，无高考出口梯队字段，
 * 故先用「接口占位」：以现有 {@code tier} 映射到三档（见 {@link GaokaoTierResolver}）。
 * 后续在 HighSchool 增加 {@code gaokaoTier} 字段后，解析实现改为直接读取即可。
 */
public enum GaokaoTier {
    /** TOP 级：清北/985 出口主力 */
    TOP,
    /** 头部：211/较强一本出口 */
    HEAD,
    /** 中上游：一本/优质本科出口 */
    MID;

    /** 校额/统招校额投放东城的跨区学校也归入可匹配范围（占位：当前数据无跨区标记） */
    public static GaokaoTier fromTier(String tier) {
        if ("KEY".equals(tier)) return TOP;
        return MID; // NORMAL 暂归入中上游；HEAD 待数据补齐后细化
    }
}
