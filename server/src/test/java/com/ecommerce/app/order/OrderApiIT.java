package com.ecommerce.app.order;

import com.ecommerce.app.address.Address;
import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
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

class OrderApiIT extends IntegrationTest {

    @Test
    void customerCanReadTheirOwnOrder() {
        AppUser alice = testData.customer();
        WebOrder order = testData.orderFor(alice, testData.productWithStock(100, 10), 1);

        ResponseEntity<String> response = call(GET, "/order/" + order.getId(), authAs(alice));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        AppUser alice = testData.customer();
        AppUser mallory = testData.customer();
        WebOrder alicesOrder = testData.orderFor(alice, testData.productWithStock(100, 10), 1);

        ResponseEntity<String> response = call(GET, "/order/" + alicesOrder.getId(), authAs(mallory));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void customerCannotDeleteOrders() {
        AppUser alice = testData.customer();
        WebOrder order = testData.orderFor(alice, testData.productWithStock(100, 10), 1);

        ResponseEntity<String> response = call(DELETE, "/order/" + order.getId(), authAs(alice));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(testData.orderExists(order)).isTrue();
    }

    @Test
    void customerCannotShipAnOrderToAnotherCustomersAddress() {
        AppUser alice = testData.customer();
        AppUser mallory = testData.customer();
        Address alicesAddress = testData.addressOf(alice);
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(authAs(mallory), alicesAddress, product.getId(), 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(testData.stockOf(product)).isEqualTo(10);
    }

    @Test
    void negativeQuantityIsRejectedAndDoesNotAddStock() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(authAs(alice), testData.addressOf(alice), product.getId(), -3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(testData.stockOf(product)).isEqualTo(10);
    }

    @Test
    void zeroQuantityIsRejected() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(authAs(alice), testData.addressOf(alice), product.getId(), 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void orderingMoreThanInStockReturnsConflictAndKeepsStock() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 2);

        ResponseEntity<String> response = placeOrder(authAs(alice), testData.addressOf(alice), product.getId(), 3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(testData.stockOf(product)).isEqualTo(2);
    }

    @Test
    void successfulOrderDecrementsStock() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(authAs(alice), testData.addressOf(alice), product.getId(), 3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testData.stockOf(product)).isEqualTo(7);
    }

    private ResponseEntity<String> placeOrder(HttpHeaders auth, Address address, Long productId, int quantity) {
        return call(POST, "/order/" + address.getId(), auth,
                List.of(Map.of("productId", productId, "quantity", quantity)));
    }
}
