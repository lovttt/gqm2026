package com.gqm2026.student.infrastructure.acl;

import com.gqm2026.student.simulator.ReferenceData;
import com.gqm2026.student.simulator.SchoolDataset;

/**
 * ACL 端口：拉取 school-service 招生资源快照（G7-Q2 后含 gaokaoTier）。
 * 应用/领域服务（QuotaEligibilityService / StudentGenerator / ApplicationSimulator / GeneratorService）
 * 只依赖本接口，RestTemplate 直连细节收敛到 SchoolReferenceRestClient 实现。
 */
public interface SchoolReferencePort {
    /** 原始快照（含各校额名额金额、控制线、gaokaoTier 等） */
    SchoolDataset fetchRaw();

    /** 转换为志愿模拟/生成所需的参考数据 */
    ReferenceData fetch();
}
