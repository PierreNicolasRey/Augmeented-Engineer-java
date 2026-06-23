package com.exalt.it.belair.application.order.dto;

import java.util.List;

public record PlaceOrderRequest(
        String festivalGoerId,
        List<OrderItemRequest> items
) {}
