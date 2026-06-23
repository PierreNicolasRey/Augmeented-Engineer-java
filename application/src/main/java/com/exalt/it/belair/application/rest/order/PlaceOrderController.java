package com.exalt.it.belair.application.order.rest;

import com.exalt.it.belair.application.order.dto.PlaceOrderRequest;
import com.exalt.it.belair.application.order.dto.PlaceOrderResponse;
import com.exalt.it.belair.application.order.mapper.PlaceOrderRequestMapper;
import com.exalt.it.belair.application.order.mapper.PlaceOrderResponseMapper;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.PlaceOrderCommand;
import com.exalt.it.belair.domain.order.ports.in.PlaceOrderUseCasePort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class PlaceOrderController {
    private final PlaceOrderUseCasePort placeOrderUseCase;

    public PlaceOrderController(PlaceOrderUseCasePort placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        PlaceOrderCommand command = PlaceOrderRequestMapper.toDomainCommand(request);
        Order order = placeOrderUseCase.placeOrder(command);
        PlaceOrderResponse response = PlaceOrderResponseMapper.fromDomainResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
