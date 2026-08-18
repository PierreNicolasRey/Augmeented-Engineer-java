package com.exalt.it.belair.application.order.rest;

import com.exalt.it.belair.application.config.GlobalErrorHandler;
import com.exalt.it.belair.application.rest.order.AcknowledgeOrderController;
import com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException;
import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeAcknowledgedException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.in.AcknowledgeOrderUseCasePort;
import com.exalt.it.belair.domain.order.ports.in.OrderQueryServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcknowledgeOrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AcknowledgeOrderUseCasePort acknowledgeOrderUseCase;

    @Mock
    private OrderQueryServicePort orderQueryService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        AcknowledgeOrderController controller = new AcknowledgeOrderController(
                acknowledgeOrderUseCase, orderQueryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Order createAcknowledgedOrder(String orderId, int drinkTokensDeducted, int snackTokensDeducted,
            int estimatedReadinessMinutes, List<OrderItem> items) {
        Order order = mock(Order.class);
        when(order.getOrderId()).thenReturn(orderId);
        when(order.getStatus()).thenReturn(OrderStatusEnum.ACKNOWLEDGED);
        when(order.getEstimatedReadinessAt()).thenReturn(LocalDateTime.now().plusMinutes(estimatedReadinessMinutes));
        when(order.getEstimatedReadinessMinutes()).thenReturn(estimatedReadinessMinutes);
        when(order.getItems()).thenReturn(items);
        when(order.getReservedDrinkTokens()).thenReturn(drinkTokensDeducted);
        when(order.getReservedSnackTokens()).thenReturn(snackTokensDeducted);
        when(order.getUpdatedAt()).thenReturn(Instant.now());
        return order;
    }

    private OrderItem createDrinkItem(String subtype, int quantity) {
        OrderItem item = mock(OrderItem.class);
        when(item.getItemType()).thenReturn("DRINK");
        when(item.getItemSubtype()).thenReturn(subtype);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    private OrderItem createFoodItem(String subtype, int quantity) {
        OrderItem item = mock(OrderItem.class);
        when(item.getItemType()).thenReturn("FOOD");
        when(item.getItemSubtype()).thenReturn(subtype);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    @Test
    void put_shouldReturn200_whenAcknowledgingPendingOrderWithSingleNormalDrink() throws Exception {
        // GIVEN
        String orderId = "ord-001";
        List<OrderItem> items = List.of(createDrinkItem("NORMAL_ALCOHOLIC", 1));
        Order acknowledgedOrder = createAcknowledgedOrder(orderId, 1, 0, 2, items);

        doNothing().when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);
        when(orderQueryService.findOrderById(orderId)).thenReturn(Optional.of(acknowledgedOrder));

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.estimatedReadinessMinutes").value(2))
                .andExpect(jsonPath("$.totalDrinkTokensDeducted").value(1))
                .andExpect(jsonPath("$.totalSnackTokensDeducted").value(0))
                .andExpect(jsonPath("$.acknowledgedAt").exists())
                .andExpect(jsonPath("$.estimatedReadinessAt").exists());
    }

    @Test
    void put_shouldReturn200_whenAcknowledgingOrderWithMultipleDrinkTypes() throws Exception {
        // GIVEN
        String orderId = "ord-002";
        // 5 non-alcoholic (5 min) + 1 normal alcoholic (2 min) + 1 premium alcoholic (3 min) = 10 min total
        List<OrderItem> items = List.of(
                createDrinkItem("NON_ALCOHOLIC", 5),
                createDrinkItem("NORMAL_ALCOHOLIC", 1),
                createDrinkItem("PREMIUM_ALCOHOLIC", 1)
        );
        Order acknowledgedOrder = createAcknowledgedOrder(orderId, 3, 0, 10, items);

        doNothing().when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);
        when(orderQueryService.findOrderById(orderId)).thenReturn(Optional.of(acknowledgedOrder));

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.estimatedReadinessMinutes").value(10))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void put_shouldReturn200_whenAcknowledgingOrderWithMixedMealsAndDrinks() throws Exception {
        // GIVEN
        String orderId = "ord-003";
        // 1 meal (10 min) + 1 premium drink (3 min, parallel) = 13 min
        List<OrderItem> items = List.of(
                createFoodItem("MEAL", 1),
                createDrinkItem("PREMIUM_ALCOHOLIC", 1)
        );
        Order acknowledgedOrder = createAcknowledgedOrder(orderId, 2, 3, 13, items);

        doNothing().when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);
        when(orderQueryService.findOrderById(orderId)).thenReturn(Optional.of(acknowledgedOrder));

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.estimatedReadinessMinutes").value(13))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void put_shouldReturn404_whenOrderNotFound() throws Exception {
        // GIVEN
        String orderId = "ord-999";
        doThrow(new OrderNotFoundException("Order not found: " + orderId))
                .when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void put_shouldReturn409_whenOrderCannotBeAcknowledged() throws Exception {
        // GIVEN
        String orderId = "ord-005";
        doThrow(new OrderCannotBeAcknowledgedException(
                "Order cannot be acknowledged from status: ACKNOWLEDGED"))
                .when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ORDER_CANNOT_BE_ACKNOWLEDGED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void put_shouldReturn404_whenFestivalGoerNotFound() throws Exception {
        // GIVEN
        String orderId = "ord-008";
        doThrow(new FestivalGoerNotFoundException("Festival goer not found"))
                .when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FESTIVAL_GOER_NOT_FOUND"));
    }

    @Test
    void put_shouldReturn200WithItemsBreakdown_whenOrderHasThreeItems() throws Exception {
        // GIVEN
        String orderId = "ord-009";
        List<OrderItem> items = List.of(
                createDrinkItem("NON_ALCOHOLIC", 2),
                createDrinkItem("NORMAL_ALCOHOLIC", 1),
                createFoodItem("SNACK", 3)
        );
        Order acknowledgedOrder = createAcknowledgedOrder(orderId, 1, 3, 7, items);

        doNothing().when(acknowledgeOrderUseCase).acknowledgeOrder(orderId);
        when(orderQueryService.findOrderById(orderId)).thenReturn(Optional.of(acknowledgedOrder));

        // WHEN & THEN
        mockMvc.perform(put("/api/v1/orders/" + orderId + "/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].itemType").value("DRINK"))
                .andExpect(jsonPath("$.items[2].itemType").value("FOOD"));
    }
}
