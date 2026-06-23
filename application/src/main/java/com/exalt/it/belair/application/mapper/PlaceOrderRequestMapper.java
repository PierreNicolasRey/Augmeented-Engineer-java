package com.exalt.it.belair.application.order.mapper;

import com.exalt.it.belair.application.order.dto.OrderItemRequest;
import com.exalt.it.belair.application.order.dto.PlaceOrderRequest;
import com.exalt.it.belair.domain.order.model.OrderItemCommand;
import com.exalt.it.belair.domain.order.model.PlaceOrderCommand;
import java.util.List;

public class PlaceOrderRequestMapper {
    public static PlaceOrderCommand toDomainCommand(PlaceOrderRequest request) {
        List<OrderItemCommand> items = request.items().stream()
                .map(itemRequest -> new OrderItemCommand(
                        itemRequest.itemType(),
                        itemRequest.itemSubtype(),
                        itemRequest.quantity()
                ))
                .toList();
        return new PlaceOrderCommand(request.festivalGoerId(), items);
    }
}
