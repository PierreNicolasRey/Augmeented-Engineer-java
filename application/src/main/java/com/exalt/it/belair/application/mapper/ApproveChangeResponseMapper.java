package com.exalt.it.belair.application.mapper;

import com.exalt.it.belair.application.dto.ApproveChangeResponseDTO;
import com.exalt.it.belair.domain.order.model.ApproveChangeReadModel;

/**
 * Mapper for converting Domain ApproveChangeReadModel to REST ApproveChangeResponseDTO.
 * Handles the conversion from rich domain read models to flat response structures
 * suitable for HTTP responses.
 */
public class ApproveChangeResponseMapper {
    
    /**
     * Converts a Domain ApproveChangeReadModel to a REST response DTO.
     *
     * @param readModel the domain ApproveChangeReadModel to convert
     * @return the response DTO containing approve change details formatted for REST clients
     */
    public static ApproveChangeResponseDTO fromDomainResponse(ApproveChangeReadModel readModel) {
        return new ApproveChangeResponseDTO(
                readModel.orderId(),
                readModel.status(),
                readModel.newEstimatedReadinessAt(),
                readModel.newEstimatedReadinessMinutes(),
                readModel.message(),
                readModel.approvedAt()
        );
    }
}