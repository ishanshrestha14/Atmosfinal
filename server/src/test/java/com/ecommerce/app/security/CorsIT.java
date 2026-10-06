package com.ecommerce.app.security;

import com.ecommerce.app.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.OPTIONS;

/**
 * Browsers send an unauthenticated preflight before every cross-origin request that carries a
 * token. If the preflight is rejected, the storefront cannot call any protected endpoint.
 */
class CorsIT extends IntegrationTest {

    @Test
    void preflightFromTheStorefrontIsAllowedOnProtectedEndpoints() {
        ResponseEntity<String> response = call(OPTIONS, "/delivery", preflightFrom("http://localhost:3000"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() {
        ResponseEntity<String> response = call(OPTIONS, "/delivery", preflightFrom("https://evil.example"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    private static HttpHeaders preflightFrom(String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origin);
        headers.setAccessControlRequestMethod(org.springframework.http.HttpMethod.POST);
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
        return headers;
    }
}
