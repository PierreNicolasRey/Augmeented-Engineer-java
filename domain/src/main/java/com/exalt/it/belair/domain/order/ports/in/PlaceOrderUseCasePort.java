package com.exalt.it.belair.domain.order.ports.in;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.PlaceOrderCommand;

public interface PlaceOrderUseCasePort {
    Order placeOrder(PlaceOrderCommand command);
}
