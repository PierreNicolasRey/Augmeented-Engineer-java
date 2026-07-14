package com.exalt.it.belair.application.rest.order;

import com.exalt.it.belair.application.dto.AcknowledgeOrderResponse;
import com.exalt.it.belair.application.mapper.AcknowledgeOrderResponseMapper;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.ports.in.AcknowledgeOrderUseCasePort;
import com.exalt.it.belair.domain.order.ports.in.OrderQueryServicePort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for acknowledging orders.
 * Maps PUT /api/v1/orders/{orderId}/acknowledge to the AcknowledgeOrderUseCasePort.
 * Exception handling is delegated to the GlobalErrorHandler.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class AcknowledgeOrderController {

    private final AcknowledgeOrderUseCasePort acknowledgeOrderUseCase;
    private final OrderQueryServicePort orderQueryService;

    public AcknowledgeOrderController(
            AcknowledgeOrderUseCasePort acknowledgeOrderUseCase,
            OrderQueryServicePort orderQueryService) {
        this.acknowledgeOrderUseCase = acknowledgeOrderUseCase;
        this.orderQueryService = orderQueryService;
    }

    /**
     * Acknowledges a pending order, transitioning it to ACKNOWLEDGED state.
     * Deducts the reserved tokens from the festival goer's balance and calculates
     * the estimated readiness time.
     *
     * @param orderId the unique identifier of the order to acknowledge
     * @return HTTP 200 with the acknowledged order details
     */
    @PutMapping("/{orderId}/acknowledge")
    public ResponseEntity<AcknowledgeOrderResponse> acknowledgeOrder(@PathVariable String orderId) {
        acknowledgeOrderUseCase.acknowledgeOrder(orderId);

        Order acknowledgedOrder = orderQueryService.findOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order " + orderId + " not found"));

        AcknowledgeOrderResponse response = AcknowledgeOrderResponseMapper.toDTO(acknowledgedOrder);
        return ResponseEntity.ok(response);
    }
}

