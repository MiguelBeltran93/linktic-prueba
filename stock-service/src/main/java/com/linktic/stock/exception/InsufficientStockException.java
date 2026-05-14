package com.linktic.stock.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, Integer available) {
        super("Insufficient stock for productId: " + productId + ". Available: " + available);
    }
}
