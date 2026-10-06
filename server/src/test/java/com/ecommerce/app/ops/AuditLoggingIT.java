package com.ecommerce.app.ops;

import com.ecommerce.app.address.Address;
import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;

/** Audit logs record what happened, never the personal data it happened to. */
@ExtendWith(OutputCaptureExtension.class)
class AuditLoggingIT extends IntegrationTest {

    @Test
    void placingAnOrderIsAuditedWithoutPersonalData(CapturedOutput output) {
        AppUser alice = testData.customer();
        Address address = testData.addressOf(alice);
        Product product = testData.productWithStock(100, 10);

        var response = call(POST, "/order/" + address.getId(), authAs(alice),
                List.of(Map.of("productId", product.getId(), "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output.getOut()).contains("OrderService.addOrder");
        assertThat(output.getOut())
                .doesNotContain(alice.getEmail())
                .doesNotContain(address.getAddressLine1());
    }
}
