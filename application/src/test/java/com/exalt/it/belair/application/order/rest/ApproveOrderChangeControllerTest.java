package com.exalt.it.belair.application.order.rest;

import com.exalt.it.belair.application.config.GlobalErrorHandler;
import com.exalt.it.belair.application.dto.ApproveChangeResponseDTO;
import com.exalt.it.belair.application.rest.order.ApproveOrderChangeController;
import com.exalt.it.belair.domain.order.model.ApproveChangeReadModel;
import com.exalt.it.belair.domain.order.ports.in.ApproveOrderChangeUseCasePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApproveOrderChangeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ApproveOrderChangeUseCasePort approveOrderChangeUseCase;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ApproveOrderChangeController controller = new ApproveOrderChangeController(approveOrderChangeUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void post_shouldReturn200_whenApproveOrderChangeRequestIsValid() throws Exception {
        // GIVEN
        String orderId = "ord-001";
        LocalDateTime estimatedReadinessAt = LocalDateTime.now().plusHours(1);
        ApproveChangeReadModel mockResponse = new ApproveChangeReadModel(
                orderId,
                "ACKNOWLEDGED",
                estimatedReadinessAt,
                60,
                "Change approved",
                LocalDateTime.now()
        );
        when(approveOrderChangeUseCase.approveChange(anyString(), anyString()))
                .thenReturn(mockResponse);

        // WHEN & THEN: POST to /api/v1/orders/{orderId}/changes/approve
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/changes/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Bartender-Id", "bartender-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.message").value("Change approved"))
                .andExpect(jsonPath("$.newEstimatedReadinessAt").exists())
                .andExpect(jsonPath("$.newEstimatedReadinessMinutes").value(60))
                .andExpect(jsonPath("$.approvedAt").exists());
    }
}
