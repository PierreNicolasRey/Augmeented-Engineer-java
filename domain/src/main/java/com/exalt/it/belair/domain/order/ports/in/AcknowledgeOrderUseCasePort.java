package com.exalt.it.belair.domain.order.ports.in;

public interface AcknowledgeOrderUseCasePort {
    void acknowledgeOrder(String orderId);
}
