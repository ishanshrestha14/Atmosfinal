package com.ecommerce.app.ops;

import com.ecommerce.app.support.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The demo credentials published in the README must work against the seeded database. */
class DemoAccountsIT extends IntegrationTest {

    @ParameterizedTest
    @CsvSource({"demo, Demo1234!", "staff, Staff1234!"})
    void documentedDemoAccountsCanSignIn(String username, String password) {
        ResponseEntity<Map> response = http.postForEntity("/login",
                Map.of("username", username, "password", password), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("access_token");
    }
}
