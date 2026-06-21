package com.exalt.it.belair.application.order.rest;

import com.exalt.it.belair.application.order.dto.PlaceOrderRequest;
import com.exalt.it.belair.application.order.dto.PlaceOrderResponse;
import com.exalt.it.belair.domain.order.ports.PlaceOrderUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class PlaceOrderController {
    private final PlaceOrderUseCase placeOrderUseCase;

    public PlaceOrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        Object result = placeOrderUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body((PlaceOrderResponse) result);
    }
}
