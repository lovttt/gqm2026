package com.gqm2026.student.simulator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 拉取 school-service 导出快照并转为 ReferenceData */
@Component
@RequiredArgsConstructor
public class SchoolDataFetcher {

    @Qualifier("authedRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${app.school-service.base-url}")
    private String schoolBaseUrl;

    public ReferenceData fetch() {
        return ReferenceData.from(fetchRaw());
    }

    /** 拉取原始 school 快照（含各校额名额金额与控制线），供校额资格重算使用。 */
    public SchoolDataset fetchRaw() {
        return restTemplate.getForObject(
                schoolBaseUrl + "/school/export", SchoolDataset.class);
    }
}
