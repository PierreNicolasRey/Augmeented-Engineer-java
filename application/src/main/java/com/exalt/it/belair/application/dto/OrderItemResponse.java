package com.exalt.it.belair.application.dto;

public record OrderItemResponse(
        String itemType,
        String itemSubtype,
        int quantity
) {}
