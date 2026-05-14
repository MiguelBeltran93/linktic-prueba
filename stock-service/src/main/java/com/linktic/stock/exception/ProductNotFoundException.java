package com.linktic.stock.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("No product found in catalog with id: " + id);
    }
}
