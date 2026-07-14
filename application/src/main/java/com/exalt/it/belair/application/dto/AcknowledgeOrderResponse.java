package com.exalt.it.belair.application.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for acknowledging an order.
 * Contains the updated order state, estimated readiness time, and token deduction breakdown.
 */
public record AcknowledgeOrderResponse(
        String orderId,
        String status,
        LocalDateTime estimatedReadinessAt,
        int estimatedReadinessMinutes,
        List<OrderItemResponse> items,
        int totalDrinkTokensDeducted,
        int totalSnackTokensDeducted,
        Instant acknowledgedAt
) {}
