package com.gqm2026.student.infrastructure.acl;

import com.gqm2026.student.simulator.ReferenceData;
import com.gqm2026.student.simulator.SchoolDataset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** SchoolReferencePort 的 REST 实现（原 SchoolDataFetcher），经 authedRestTemplate 拉取 school-service 快照。 */
@Component
@RequiredArgsConstructor
public class SchoolReferenceRestClient implements SchoolReferencePort {

    @Qualifier("authedRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${app.school-service.base-url}")
    private String schoolBaseUrl;

    @Override
    public SchoolDataset fetchRaw() {
        return restTemplate.getForObject(schoolBaseUrl + "/school/export", SchoolDataset.class);
    }

    @Override
    public ReferenceData fetch() {
        return ReferenceData.from(fetchRaw());
    }
}
