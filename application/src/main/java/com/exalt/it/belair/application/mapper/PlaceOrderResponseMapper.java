package com.exalt.it.belair.application.mapper;

import com.exalt.it.belair.application.dto.OrderItemResponse;
import com.exalt.it.belair.application.dto.PlaceOrderResponse;
import com.exalt.it.belair.domain.order.model.Order;
import java.util.List;

/**
 * Mapper for converting Domain Order models to REST PlaceOrderResponse DTOs.
 * Handles the conversion from rich domain entities to flat response structures
 * suitable for HTTP responses.
 */
public class PlaceOrderResponseMapper {
    
    /**
     * Converts a Domain Order to a REST response DTO.
     *
     * @param order the domain Order to convert
     * @return the response DTO containing order details formatted for REST clients
     */
    public static PlaceOrderResponse fromDomainResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getItemType(),
                        item.getItemSubtype(),
                        item.getQuantity()
                ))
                .toList();
        
        return new PlaceOrderResponse(
                order.getOrderId(),
                order.getFestivalGoerId(),
                order.getStatus().toString(),
                items,
                order.getDrinkTokenCost(),
                order.getSnackTokenCost()
        );
    }
}
