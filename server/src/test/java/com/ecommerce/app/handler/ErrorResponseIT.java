package com.ecommerce.app.handler;

import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/** Every error is an RFC 7807 problem detail, whichever layer produced it. */
class ErrorResponseIT extends IntegrationTest {

    @Test
    void validationErrorsListEveryViolation() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = call(POST, "/order/" + testData.addressOf(alice).getId(), authAs(alice),
                List.of(Map.of("productId", product.getId(), "quantity", 0)));

        assertProblem(response, HttpStatus.BAD_REQUEST);
        List<String> errors = JsonPath.read(response.getBody(), "$.errors");
        assertThat(errors).containsExactly("Quantity cannot be 0 or negative");
    }

    @Test
    void invalidRequestBodyFieldsAreListed() {
        ResponseEntity<String> response = call(POST, "/register", new HttpHeaders(),
                Map.of("username", "x", "email", "not-an-email", "password", "Passw0rd!", "confirmPassword", "Passw0rd!"));

        assertProblem(response, HttpStatus.BAD_REQUEST);
        List<String> errors = JsonPath.read(response.getBody(), "$.errors");
        assertThat(errors).contains("Email is not well formatted", "Username should be from 3 to 255 symbols");
    }

    @Test
    void registrationWithMismatchedPasswordsIsABadRequest() {
        ResponseEntity<String> response = call(POST, "/register", new HttpHeaders(),
                Map.of("username", "mismatch-user", "email", "mismatch@test.local",
                        "password", "Passw0rd!", "confirmPassword", "Different1!"));

        assertProblem(response, HttpStatus.BAD_REQUEST);
        assertThat((Integer) JsonPath.read(response.getBody(), "$.errorCode")).isEqualTo(4);
    }

    @Test
    void malformedJsonIsABadRequest() {
        HttpHeaders headers = authAs(testData.customer());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = call(POST, "/delivery", headers, "{not json");

        assertProblem(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingResourceIsNotFound() {
        ResponseEntity<String> response = call(GET, "/order/999999", authAs(testData.customer()));

        assertProblem(response, HttpStatus.NOT_FOUND);
        assertThat((String) JsonPath.read(response.getBody(), "$.detail")).isEqualTo("Order 999999 not found");
    }

    @Test
    void missingProductForStaffIsNotFound() {
        ResponseEntity<String> response = call(GET, "/staff/product/999999", authAs(testData.staff()));

        assertProblem(response, HttpStatus.NOT_FOUND);
    }

    @Test
    void insufficientStockIsAConflict() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 1);

        ResponseEntity<String> response = call(POST, "/order/" + testData.addressOf(alice).getId(), authAs(alice),
                List.of(Map.of("productId", product.getId(), "quantity", 2)));

        assertProblem(response, HttpStatus.CONFLICT);
    }

    @Test
    void wrongPasswordIsUnauthorizedWithErrorCode() {
        AppUser alice = testData.customer();

        ResponseEntity<String> response = call(POST, "/login", new HttpHeaders(),
                Map.of("username", alice.getUsername(), "password", "Wrong-passw0rd!"));

        assertProblem(response, HttpStatus.UNAUTHORIZED);
        assertThat((Integer) JsonPath.read(response.getBody(), "$.errorCode")).isEqualTo(1);
    }

    @Test
    void missingTokenOnProtectedEndpointIsUnauthorized() {
        ResponseEntity<String> response = call(GET, "/order/all", new HttpHeaders());

        assertProblem(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void insufficientRoleIsForbidden() {
        Product product = testData.productWithStock(100, 1);

        ResponseEntity<String> response = call(DELETE, "/staff/product/" + product.getId(), authAs(testData.customer()));

        assertProblem(response, HttpStatus.FORBIDDEN);
    }

    private static void assertProblem(ResponseEntity<String> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        assertThat((Integer) JsonPath.read(response.getBody(), "$.status")).isEqualTo(status.value());
        assertThat((String) JsonPath.read(response.getBody(), "$.title")).isNotBlank();
    }
}
