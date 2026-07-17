package com.gqm2026.admission.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** 以管理员身份向 auth-service 换取 JWT，缓存后供服务间调用使用 */
@Component
@RequiredArgsConstructor
public class AuthTokenProvider {

    @Value("${app.auth-service.base-url}")
    private String authBaseUrl;

    @Value("${app.admin.username}")
    private String adminUser;

    @Value("${app.admin.password}")
    private String adminPass;

    private String cachedToken;

    public String getToken() {
        if (cachedToken == null) {
            refresh();
        }
        return cachedToken;
    }

    public void refresh() {
        RestTemplate restTemplate = new RestTemplate();
        Map<?, ?> resp = restTemplate.postForObject(
                authBaseUrl + "/auth/login",
                Map.of("username", adminUser, "password", adminPass),
                Map.class);
        cachedToken = (String) resp.get("token");
    }
}
