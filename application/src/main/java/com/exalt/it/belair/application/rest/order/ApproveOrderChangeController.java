package com.exalt.it.belair.application.rest.order;

import com.exalt.it.belair.application.dto.ApproveChangeResponseDTO;
import com.exalt.it.belair.application.mapper.ApproveChangeResponseMapper;
import com.exalt.it.belair.domain.order.model.ApproveChangeReadModel;
import com.exalt.it.belair.domain.order.ports.in.ApproveOrderChangeUseCasePort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for approving order changes.
 * Maps HTTP requests to the ApproveOrderChangeUseCasePort and returns responses as DTOs.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class ApproveOrderChangeController {
    private final ApproveOrderChangeUseCasePort approveOrderChangeUseCase;

    public ApproveOrderChangeController(ApproveOrderChangeUseCasePort approveOrderChangeUseCase) {
        this.approveOrderChangeUseCase = approveOrderChangeUseCase;
    }

    @PostMapping("/{orderId}/changes/approve")
    public ResponseEntity<ApproveChangeResponseDTO> approveChange(
            @PathVariable String orderId,
            @RequestHeader("X-Bartender-Id") String bartenderId) {
        
        ApproveChangeReadModel readModel = approveOrderChangeUseCase.approveChange(orderId, bartenderId);
        ApproveChangeResponseDTO response = ApproveChangeResponseMapper.fromDomainResponse(readModel);
        
        return ResponseEntity.ok(response);
    }
}
