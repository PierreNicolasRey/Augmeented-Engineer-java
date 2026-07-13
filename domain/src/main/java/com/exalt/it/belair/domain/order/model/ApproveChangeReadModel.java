package com.exalt.it.belair.domain.order.model;

import java.time.LocalDateTime;

/**
 * Read Model representing the result of approving an order change.
 * This immutable record is returned by the ApproveOrderChangeUseCasePort.
 */
public record ApproveChangeReadModel(
        String orderId,
        String status,
        LocalDateTime newEstimatedReadinessAt,
        int newEstimatedReadinessMinutes,
        String message,
        LocalDateTime approvedAt
) {}
