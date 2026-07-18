package com.gqm2026.admission.infrastructure.acl;

import com.gqm2026.admission.dto.StudentSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * student-service 快照端口实现：经 authedRestTemplate（附 admin JWT）直连 {@code GET {student}/student/export}。
 * RestTemplate 仅出现在本实现类（G7-Q2 端口化）。
 */
@Service
public class StudentSnapshotRestClient implements StudentSnapshotPort {

    private final RestTemplate restTemplate;

    @Value("${app.student-service.base-url}")
    private String studentBaseUrl;

    public StudentSnapshotRestClient(@Qualifier("authedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public StudentSnapshot fetch() {
        return restTemplate.getForObject(studentBaseUrl + "/student/export", StudentSnapshot.class);
    }
}
