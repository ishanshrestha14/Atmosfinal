package com.ecommerce.app.support;

import com.ecommerce.app.user.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

/**
 * Base class for tests that run the full application against real infrastructure:
 * PostgreSQL for persistence and Mailpit as the SMTP server.
 * <p>
 * Containers are started once per JVM and shared by every test class, so the cached
 * Spring context always points at live containers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestData.class)
public abstract class IntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withExposedPorts(1025, 8025);

    static {
        POSTGRES.start();
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @Autowired
    protected TestData testData;

    @Autowired
    protected TestRestTemplate http;

    /** Logs in through the real /login endpoint and returns headers carrying the access token. */
    protected HttpHeaders authAs(AppUser user) {
        ResponseEntity<Map> login = http.postForEntity("/login",
                Map.of("username", user.getUsername(), "password", TestData.PASSWORD), Map.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) login.getBody().get("access_token"));
        return headers;
    }

    protected ResponseEntity<String> call(HttpMethod method, String path, HttpHeaders headers, Object body) {
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    protected ResponseEntity<String> call(HttpMethod method, String path, HttpHeaders headers) {
        return call(method, path, headers, null);
    }

    /** Number of emails Mailpit has received for the given recipient. */
    protected int emailsReceivedBy(String recipient) {
        String url = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025)
                + "/api/v1/search?query=to:\"" + recipient + "\"";
        Map<?, ?> result = http.getForObject(url, Map.class);
        return ((Number) result.get("messages_count")).intValue();
    }
}
