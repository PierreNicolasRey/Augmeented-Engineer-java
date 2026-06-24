package com.exalt.it.belair.application.dto;

public record OrderItemRequest(
        String itemType,
        String itemSubtype,
        int quantity
) {}
