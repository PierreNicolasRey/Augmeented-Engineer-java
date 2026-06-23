package com.exalt.it.belair.application.order.dto;

public record OrderItemRequest(
        String itemType,
        String itemSubtype,
        int quantity
) {}
