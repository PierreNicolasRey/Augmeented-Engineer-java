package com.exalt.it.belair.application.mapper;

import com.exalt.it.belair.application.dto.AcknowledgeOrderResponse;
import com.exalt.it.belair.application.dto.OrderItemResponse;
import com.exalt.it.belair.domain.order.model.Order;
import java.util.List;

/**
 * Mapper for converting Domain Order models to REST AcknowledgeOrderResponse DTOs.
 * Handles the conversion from rich domain entities to flat response structures
 * suitable for HTTP responses.
 * No business logic is performed here: time calculations are already done in the domain.
 */
public class AcknowledgeOrderResponseMapper {

    /**
     * Converts a Domain Order to a REST acknowledge response DTO.
     * The order must already be in ACKNOWLEDGED state with estimatedReadinessMinutes set.
     *
     * @param order the acknowledged domain Order to convert
     * @return the response DTO containing acknowledge details formatted for REST clients
     */
    public static AcknowledgeOrderResponse toDTO(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getItemType(),
                        item.getItemSubtype(),
                        item.getQuantity()
                ))
                .toList();

        return new AcknowledgeOrderResponse(
                order.getOrderId(),
                order.getStatus().toString(),
                order.getEstimatedReadinessAt(),
                order.getEstimatedReadinessMinutes(),
                items,
                order.getReservedDrinkTokens(),
                order.getReservedSnackTokens(),
                order.getUpdatedAt()
        );
    }
}
