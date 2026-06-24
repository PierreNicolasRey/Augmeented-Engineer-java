package com.exalt.it.belair.domain.order.exceptions;

public class ItemNotFoundInCatalogException extends RuntimeException {
    public ItemNotFoundInCatalogException(String message) {
        super(message);
    }
}
