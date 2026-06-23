package com.exalt.it.belair.application.order.mapper;

import com.exalt.it.belair.application.order.dto.OrderItemResponse;
import com.exalt.it.belair.application.order.dto.PlaceOrderResponse;
import com.exalt.it.belair.domain.order.model.Order;
import java.util.List;

public class PlaceOrderResponseMapper {
    public static PlaceOrderResponse fromDomainResponse(Order order) {
        // For now, returning a dummy response structure
        // Will be implemented when domain Order has the required fields
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getItemType(),
                        item.getItemSubtype(),
                        item.getQuantity()
                ))
                .toList();
        return new PlaceOrderResponse(
                "order-001",  // TODO: get from order when implemented
                "fgv-001",    // TODO: get from order when implemented
                "PENDING",    // TODO: get from order.getStatus()
                items,
                order.getDrinkTokenCost(),
                order.getSnackTokenCost()
        );
    }
}
