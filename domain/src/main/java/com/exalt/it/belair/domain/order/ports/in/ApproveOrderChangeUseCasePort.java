package com.exalt.it.belair.domain.order.ports.in;

import com.exalt.it.belair.domain.order.model.ApproveChangeReadModel;

public interface ApproveOrderChangeUseCasePort {
    ApproveChangeReadModel approveChange(String orderId, String bartenderId);
}

