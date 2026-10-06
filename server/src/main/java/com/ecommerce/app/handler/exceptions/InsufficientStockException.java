package com.ecommerce.app.handler.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId) {
        super("Not enough stock for product " + productId);
    }
}
