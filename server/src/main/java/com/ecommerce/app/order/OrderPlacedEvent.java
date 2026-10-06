package com.ecommerce.app.order;

import com.ecommerce.app.email.OrderItem;

import java.util.List;

/** Published when an order is placed. Carries plain values so listeners never touch JPA entities. */
public record OrderPlacedEvent(String customerEmail, String customerUsername, List<OrderItem> items, int totalPrice) {
}
