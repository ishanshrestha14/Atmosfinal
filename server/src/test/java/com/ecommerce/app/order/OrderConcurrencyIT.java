package com.ecommerce.app.order;

import com.ecommerce.app.address.Address;
import com.ecommerce.app.product.Product;
import com.ecommerce.app.support.IntegrationTest;
import com.ecommerce.app.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConcurrencyIT extends IntegrationTest {

    private static final int STOCK = 5;
    private static final int BUYERS = 20;

    @Autowired
    private OrderService orderService;

    @Test
    void concurrentCheckoutsNeverSellMoreThanAvailableStock() throws Exception {
        Product product = testData.productWithStock(100, STOCK);
        List<Callable<Boolean>> buyers = new ArrayList<>();
        CountDownLatch startGate = new CountDownLatch(1);

        for (int i = 0; i < BUYERS; i++) {
            AppUser buyer = testData.customer();
            Address address = testData.addressOf(buyer);
            buyers.add(() -> {
                startGate.await();
                try {
                    orderService.addOrder(buyer, List.of(new WebOrderContentDTO(product.getId(), 1)), address.getId());
                    return true;
                } catch (RuntimeException rejected) {
                    return false;
                }
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        try {
            List<Future<Boolean>> results = buyers.stream().map(pool::submit).toList();
            startGate.countDown();

            long successfulOrders = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successfulOrders++;
            }

            assertThat(successfulOrders).isEqualTo(STOCK);
            assertThat(testData.stockOf(product)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }
}
