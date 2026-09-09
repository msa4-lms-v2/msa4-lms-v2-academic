package com.msa4lmsv2academic.domain.admission.client;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AdmissionAccountClient {
    private final RestClient client;
    public AdmissionAccountClient(@Value("${services.auth.url:http://localhost:8081}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public void createAccount(Map<String, Object> payload) {
        Map<String, Object> request = new LinkedHashMap<>(payload);
        Object administratorId = request.remove("administratorId");
        client.post().uri("/api/auth/accounts/admission-candidates")
                .header("X-User-Id", String.valueOf(administratorId))
                .header("X-User-Role", "ADMIN")
                .body(request).retrieve().toBodilessEntity();
    }
}
