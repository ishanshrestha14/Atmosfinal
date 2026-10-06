package com.ecommerce.app.order;

import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.support.TestData;
import com.ecommerce.app.user.AppUser;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * The confirmation email is a side effect of a placed order, not part of it: mail problems must
 * never lose an order, and the email must only describe orders that were actually committed.
 */
class OrderEmailIT extends IntegrationTest {

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreMailServer() {
        reset(mailSender);
    }

    @Test
    void placedOrderSendsConfirmationEmail() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(alice, product, 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(emailsReceivedBy(alice.getEmail())).isEqualTo(1));
    }

    @Test
    void confirmationEmailIsSentFromTheConfiguredSender() {
        AppUser alice = testData.customer();

        placeOrder(alice, testData.productWithStock(100, 10), 1);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(emailsReceivedBy(alice.getEmail())).isEqualTo(1));
        assertThat(latestEmailSenderFor(alice.getEmail())).isEqualTo(TestData.MAIL_FROM);
    }

    @Test
    void confirmationEmailLinksProductImagesOnTheStorefront() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        placeOrder(alice, product, 1);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(emailsReceivedBy(alice.getEmail())).isEqualTo(1));
        assertThat(latestEmailHtmlFor(alice.getEmail()))
                .contains("src=\"http://localhost:3000" + product.getFilePath() + "\"");
    }

    @Test
    void orderIsPlacedEvenWhenTheMailServerIsDown() {
        doThrow(new MailSendException("SMTP server unavailable")).when(mailSender).send(any(MimeMessage.class));
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);

        ResponseEntity<String> response = placeOrder(alice, product, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testData.stockOf(product)).isEqualTo(8);
        assertThat(call(GET, "/order/all", authAs(alice)).getBody()).contains(product.getName());
    }

    @Test
    void rejectedOrderSendsNoEmail() {
        AppUser alice = testData.customer();
        Product inStock = testData.productWithStock(100, 10);
        Product soldOut = testData.productWithStock(100, 0);

        ResponseEntity<String> response = call(POST, "/order/" + testData.addressOf(alice).getId(), authAs(alice),
                List.of(Map.of("productId", inStock.getId(), "quantity", 1),
                        Map.of("productId", soldOut.getId(), "quantity", 1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(testData.stockOf(inStock)).isEqualTo(10);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(emailsReceivedBy(alice.getEmail())).isZero());
    }

    @Test
    void orderThatFailsToCommitSendsNoEmail() {
        AppUser alice = testData.customer();
        Product product = testData.productWithStock(100, 10);
        failCommitsOfOrdersBy(alice);
        try {
            ResponseEntity<String> response = placeOrder(alice, product, 1);

            assertThat(response.getStatusCode().is5xxServerError()).isTrue();
            assertThat(testData.stockOf(product)).isEqualTo(10);
            await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(emailsReceivedBy(alice.getEmail())).isZero());
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS fail_order_commit ON web_order");
        }
    }

    /**
     * Installs a deferred constraint trigger, so PostgreSQL rejects the transaction at COMMIT time:
     * after the application has done all of its work, exactly like a real commit failure.
     */
    private void failCommitsOfOrdersBy(AppUser user) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION fail_order_commit() RETURNS trigger AS $$
                BEGIN
                    IF NEW.user_id = %d THEN
                        RAISE EXCEPTION 'simulated commit failure';
                    END IF;
                    RETURN NEW;
                END
                $$ LANGUAGE plpgsql
                """.formatted(user.getId()));
        jdbc.execute("""
                CREATE CONSTRAINT TRIGGER fail_order_commit AFTER INSERT ON web_order
                DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fail_order_commit()
                """);
    }

    private ResponseEntity<String> placeOrder(AppUser customer, Product product, int quantity) {
        return call(POST, "/order/" + testData.addressOf(customer).getId(), authAs(customer),
                List.of(Map.of("productId", product.getId(), "quantity", quantity)));
    }
}
