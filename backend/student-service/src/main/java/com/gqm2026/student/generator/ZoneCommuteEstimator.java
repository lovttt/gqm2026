package com.gqm2026.student.generator;

import org.springframework.stereotype.Component;

/**
 * 基于片区(zone)距离的通勤时长近似实现。
 *
 * <p>同片区视为校内/就近，基准 15 分钟；每跨一个片区增加 8 分钟通勤。
 * 这是「实时交通数据」缺失下的占位近似，仅用于生成器的通勤上限过滤与权重排序。
 */
@Component
public class ZoneCommuteEstimator implements CommuteEstimator {

    private static final int BASE_MINUTES = 15;
    private static final int PER_ZONE_MINUTES = 8;

    @Override
    public int estimateMinutes(long juniorSchoolId, int juniorZone,
                               long highSchoolId, int highZone) {
        int zoneDistance = Math.abs(juniorZone - highZone);
        return BASE_MINUTES + zoneDistance * PER_ZONE_MINUTES;
    }
}
