package com.ecommerce.app.product;

import com.ecommerce.app.order.WebOrder;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

class PricingIT extends IntegrationTest {

    @Test
    void productPricesKeepTheirCents() {
        ResponseEntity<String> created = call(POST, "/staff/product", authAs(testData.staff()), productBody("19.99"));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(decimal(created.getBody(), "$.price")).isEqualByComparingTo("19.99");
    }

    @Test
    void negativePriceIsRejected() {
        ResponseEntity<String> response = call(POST, "/staff/product", authAs(testData.staff()), productBody("-5.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void orderKeepsThePricePaidWhenTheProductIsRepricedLater() {
        HttpHeaders staff = authAs(testData.staff());
        String productId = JsonPath.read(call(POST, "/staff/product", staff, productBody("10.50")).getBody(), "$.id").toString();
        AppUser alice = testData.customer();
        HttpHeaders aliceAuth = authAs(alice);

        String orderJson = call(POST, "/order/" + testData.addressOf(alice).getId(), aliceAuth,
                List.of(Map.of("productId", Long.valueOf(productId), "quantity", 2))).getBody();
        String orderId = JsonPath.read(orderJson, "$.id").toString();
        Map<String, Object> repriced = productBody("99.00");
        repriced.put("name", JsonPath.read(call(GET, "/staff/product/" + productId, staff).getBody(), "$.name"));
        repriced.put("filePath", JsonPath.read(call(GET, "/staff/product/" + productId, staff).getBody(), "$.filePath"));
        assertThat(call(PUT, "/staff/product/" + productId, staff, repriced).getStatusCode()).isEqualTo(HttpStatus.OK);

        String order = call(GET, "/order/" + orderId, aliceAuth).getBody();

        assertThat(decimal(order, "$.contents[0].unitPrice")).isEqualByComparingTo("10.50");
        assertThat(decimal(order, "$.total")).isEqualByComparingTo("21.00");
    }

    private static Map<String, Object> productBody(String price) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> body = new HashMap<>();
        body.put("name", "priced-" + suffix);
        body.put("filePath", "/img/priced-" + suffix + ".png");
        body.put("shortDescription", "short");
        body.put("longDescription", "long");
        body.put("brand", "brand");
        body.put("price", new BigDecimal(price));
        body.put("inventoryQuantity", 10);
        body.put("categoryBody", Map.of("name", "pricing"));
        return body;
    }

    /** Reads a JSON number as BigDecimal; a missing field reads as null so the assertion reports it. */
    private static BigDecimal decimal(String json, String path) {
        Object value = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
                .parse(json).read(path);
        return value == null ? null : new BigDecimal(value.toString());
    }
}
