package com.gqm2026.student.generator;

import com.gqm2026.student.simulator.ReferenceData;
import org.springframework.stereotype.Component;

/**
 * 解析某高中的高考出口梯队（占位实现）。
 *
 * <p>当前数据模型无 {@code gaokaoTier} 字段，按「KEY→TOP / NORMAL→MID」映射。
 * 预留 {@code overrideBySchoolCode} 可在不改动 DB 的前提下手工指定个别学校的梯队。
 * 待 HighSchool 增加 {@code gaokaoTier} 后改为直接读取该字段。
 */
@Component
public class GaokaoTierResolver {

    /**
     * 解析高考出口梯队。
     *
     * @param hs 高中视图（含 tier、gaokaoTier 与 code）
     */
    public GaokaoTier resolve(ReferenceData.HighSchoolView hs) {
        if (hs.gaokaoTier() != null && !hs.gaokaoTier().isBlank()) {
            try {
                return GaokaoTier.valueOf(hs.gaokaoTier());
            } catch (IllegalArgumentException ignore) {
                // 未知值回退 tier 映射
            }
        }
        return GaokaoTier.fromTier(hs.tier());
    }
}
