package com.exalt.it.belair.application.dto;

import java.time.LocalDateTime;

public record ApproveChangeResponseDTO(
        String orderId,
        String status,
        LocalDateTime newEstimatedReadinessAt,
        int newEstimatedReadinessMinutes,
        String message,
        LocalDateTime approvedAt
) {}
