package com.ecommerce.app.inventory;

import com.ecommerce.app.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProduct(Product product);

    /**
     * Atomically takes {@code quantity} units out of stock. The conditional update holds a row
     * lock until the surrounding transaction ends, so concurrent checkouts are serialised by the
     * database and stock can never go negative.
     *
     * @return 1 if the stock was reserved, 0 if there was not enough stock
     */
    @Modifying
    @Query("""
            UPDATE Inventory i
               SET i.quantity = i.quantity - :quantity
             WHERE i.product.id = :productId
               AND i.quantity >= :quantity
            """)
    int reserveStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
