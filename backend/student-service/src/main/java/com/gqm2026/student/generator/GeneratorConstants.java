package com.gqm2026.student.generator;

/**
 * 志愿生成器相关常量（见 docs/spec 与用户校验清单）。
 *
 * 注：本模块按「数据模型先接口占位」原则实现——
 * 综合素质评价、贯通培养项目批次、高考出口梯队三档等字段当前未落在 DB 实体上，
 * 这里用常量/占位接口定义契约，待数据模型补齐后仅替换解析实现即可。
 */
public final class GeneratorConstants {

    private GeneratorConstants() {}

    /** 批次类型 */
    public static final String BATCH_QUOTA = "QUOTA";       // 校额到校
    public static final String BATCH_TONGZHAO = "TONGZHAO"; // 统一招生
    public static final String BATCH_GUANTONG = "GUANTONG"; // 贯通培养项目（占位批次）

    /** 各批次志愿容量上限（用户清单：校额≤8、统招≤12；贯通按≤8占位） */
    public static final int QUOTA_CAP = 8;
    public static final int TONGZHAO_CAP = 12;
    public static final int GUANTONG_CAP = 8;

    /** 校额到校报考门槛：总分控制线（与 QuotaEligibilityService 一致，默认 430） */
    public static final int QUOTA_CONTROL_LINE = 430;
    /** 综合素质评价达标等级：B 等及以上方满足校额到校报考条件 */
    public static final String QUOTA_COMP_EVAL_MIN = "B";

    /** 贯通培养项目报考门槛：总分低于该值屏蔽全部贯通志愿（用户清单：380） */
    public static final int GUANTONG_LINE = 380;

    /** 梯度区间（按清单用「分数差」，单位：分） */
    public static final int SPRINT_DELTA_LOW = 5;   // 冲刺：比考生高 5~15 分
    public static final int SPRINT_DELTA_HIGH = 15;
    public static final int SAFETY_DELTA_LOW = 10;  // 兜底：比考生低 10~20 分
    public static final int SAFETY_DELTA_HIGH = 20;

    /** 三类梯度权重（冲刺/稳妥/兜底）之和必须等于该值 */
    public static final int WEIGHT_SUM_TARGET = 100;
}
