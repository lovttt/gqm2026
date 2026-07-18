package com.gqm2026.admission.infrastructure.acl;

import com.gqm2026.admission.dto.SchoolSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * school-service 快照端口实现：经 authedRestTemplate（附 admin JWT）直连 {@code GET {school}/school/export}。
 * RestTemplate 仅出现在本实现类（G7-Q2 端口化）。
 */
@Service
public class SchoolSnapshotRestClient implements SchoolSnapshotPort {

    private final RestTemplate restTemplate;

    @Value("${app.school-service.base-url}")
    private String schoolBaseUrl;

    public SchoolSnapshotRestClient(@Qualifier("authedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public SchoolSnapshot fetch() {
        return restTemplate.getForObject(schoolBaseUrl + "/school/export", SchoolSnapshot.class);
    }
}
