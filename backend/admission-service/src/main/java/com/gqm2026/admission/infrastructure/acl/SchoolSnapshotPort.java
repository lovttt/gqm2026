package com.gqm2026.admission.infrastructure.acl;

import com.gqm2026.admission.dto.SchoolSnapshot;

/**
 * ACL 端口：拉取 school-service 招生资源快照（高中/名额/控制线/分组）。
 * 应用/领域服务只依赖本接口；RestTemplate 直连细节收敛到 {@link SchoolSnapshotRestClient}。
 */
public interface SchoolSnapshotPort {
    SchoolSnapshot fetch();
}
