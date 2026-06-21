package com.exalt.it.belair.application.order.dto;

import java.util.List;

public record PlaceOrderResponse(
        String orderId,
        String festivalGoerId,
        String status,
        List<OrderItemResponse> items,
        int drinkTokensCost,
        int snackTokensCost
) {}
