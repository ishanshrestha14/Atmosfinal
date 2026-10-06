package com.ecommerce.app.address;

import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;

class AddressApiIT extends IntegrationTest {

    private static final Map<String, String> NEW_ADDRESS = Map.of(
            "firstName", "Mal",
            "lastName", "Lory",
            "addressLine1", "666 Attacker Road",
            "city", "Elsewhere",
            "country", "Nowhere");

    @Test
    void customerCanUpdateTheirOwnAddress() {
        AppUser alice = testData.customer();
        Address address = testData.addressOf(alice);

        ResponseEntity<String> response = call(PUT, "/delivery/" + address.getId(), authAs(alice), NEW_ADDRESS);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testData.reload(address).getAddressLine1()).isEqualTo("666 Attacker Road");
    }

    @Test
    void customerCannotUpdateAnotherCustomersAddress() {
        Address alicesAddress = testData.addressOf(testData.customer());
        AppUser mallory = testData.customer();

        ResponseEntity<String> response = call(PUT, "/delivery/" + alicesAddress.getId(), authAs(mallory), NEW_ADDRESS);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(testData.reload(alicesAddress).getAddressLine1()).isEqualTo("1 Test Street");
    }

    @Test
    void customerCannotDeleteAnotherCustomersAddress() {
        Address alicesAddress = testData.addressOf(testData.customer());
        AppUser mallory = testData.customer();

        ResponseEntity<String> response = call(DELETE, "/delivery/" + alicesAddress.getId(), authAs(mallory));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(testData.reload(alicesAddress)).isNotNull();
    }
}
