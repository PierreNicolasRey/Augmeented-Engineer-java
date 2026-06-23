package com.exalt.it.belair.application.order.dto;

public record OrderItemResponse(
        String itemType,
        String itemSubtype,
        int quantity
) {}
