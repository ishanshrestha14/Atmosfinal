package com.ecommerce.app.security;

import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/** Staff-only operations must be enforced for every role, not just declared. */
class RoleAccessIT extends IntegrationTest {

    @Test
    void anonymousUserCannotChangeStock() {
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = setStock(new HttpHeaders(), product, 0);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        assertThat(testData.stockOf(product)).isEqualTo(10);
    }

    @Test
    void customerCannotChangeStock() {
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = setStock(authAs(testData.customer()), product, 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(testData.stockOf(product)).isEqualTo(10);
    }

    @Test
    void staffCanChangeStock() {
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = setStock(authAs(testData.staff()), product, 42);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testData.stockOf(product)).isEqualTo(42);
    }

    @Test
    void anyoneCanReadStockLevels() {
        testData.productWithStock(100, 10);

        ResponseEntity<String> response = call(GET, "/inventory", new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void customerCannotDeleteProducts() {
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = call(DELETE, "/staff/product/" + product.getId(), authAs(testData.customer()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(testData.productExists(product)).isTrue();
    }

    @Test
    void customerCannotRegisterStaffAccounts() {
        Map<String, String> newStaff = Map.of(
                "username", "sneaky-staff",
                "email", "sneaky@test.local",
                "password", "Passw0rd!",
                "confirmPassword", "Passw0rd!");

        ResponseEntity<String> response = call(POST, "/staff/register", authAs(testData.customer()), newStaff);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void staffCanRegisterStaffAccounts() {
        Map<String, String> newStaff = Map.of(
                "username", "new-staff-member",
                "email", "new-staff@test.local",
                "password", "Passw0rd!",
                "confirmPassword", "Passw0rd!");

        ResponseEntity<String> response = call(POST, "/staff/register", authAs(testData.staff()), newStaff);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void malformedTokenOnPublicEndpointIsTreatedAsAnonymous() {
        HttpHeaders garbage = new HttpHeaders();
        garbage.setBearerAuth("not-a-jwt");

        ResponseEntity<String> response = call(GET, "/products", garbage);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void staffCanReadProductDetails() {
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = call(GET, "/staff/product/" + product.getId(), authAs(testData.staff()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> setStock(HttpHeaders auth, Product product, int quantity) {
        return call(PUT, "/inventory", auth, List.of(Map.of("productId", product.getId(), "quantity", quantity)));
    }
}
