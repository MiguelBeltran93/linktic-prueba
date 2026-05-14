package com.linktic.catalog.exception;

public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(Long id) {
        super("No catalog item found with id: " + id);
    }
}
