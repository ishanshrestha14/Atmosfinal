package com.ecommerce.app.ops;

import com.ecommerce.app.support.IntegrationTest;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.http.HttpMethod.GET;

/**
 * The health endpoint drives container restarts and load-balancer routing. Email is a
 * best-effort side channel, so an SMTP outage must not mark the whole API as down.
 */
class HealthIT extends IntegrationTest {

    @MockitoSpyBean
    private JavaMailSenderImpl mailSender;

    @AfterEach
    void restoreMailServer() {
        reset(mailSender);
    }

    @Test
    void apiStaysHealthyWhenTheMailServerIsUnreachable() throws MessagingException {
        doThrow(new MessagingException("SMTP server unreachable")).when(mailSender).testConnection();

        ResponseEntity<String> response = call(GET, "/actuator/health", new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
