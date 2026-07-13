package com.exalt.it.belair.domain.order.events;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Domain event published when an order has been acknowledged by a bartender.
 */
public class OrderAcknowledgedEvent {
    private final String orderId;
    private final String festivalGoerId;
    private final LocalDateTime estimatedReadinessAt;
    private final Instant acknowledgedAt;

    public OrderAcknowledgedEvent(
            String orderId,
            String festivalGoerId,
            LocalDateTime estimatedReadinessAt,
            Instant acknowledgedAt
    ) {
        this.orderId = orderId;
        this.festivalGoerId = festivalGoerId;
        this.estimatedReadinessAt = estimatedReadinessAt;
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getFestivalGoerId() {
        return festivalGoerId;
    }

    public LocalDateTime getEstimatedReadinessAt() {
        return estimatedReadinessAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
