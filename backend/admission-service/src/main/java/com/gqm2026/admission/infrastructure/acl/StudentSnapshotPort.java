package com.gqm2026.admission.infrastructure.acl;

import com.gqm2026.admission.dto.StudentSnapshot;

/**
 * ACL 端口：拉取 student-service 考生/志愿快照。
 * 应用/领域服务只依赖本接口；RestTemplate 直连细节收敛到 {@link StudentSnapshotRestClient}。
 */
public interface StudentSnapshotPort {
    StudentSnapshot fetch();
}
