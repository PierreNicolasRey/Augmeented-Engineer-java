package com.exalt.it.belair.application.order.rest;

import com.exalt.it.belair.application.config.GlobalErrorHandler;
import com.exalt.it.belair.application.dto.OrderItemRequest;
import com.exalt.it.belair.application.dto.OrderItemResponse;
import com.exalt.it.belair.application.dto.PlaceOrderRequest;
import com.exalt.it.belair.application.dto.PlaceOrderResponse;
import com.exalt.it.belair.application.rest.order.PlaceOrderController;
import com.exalt.it.belair.domain.order.exceptions.EmptyOrderException;
import com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException;
import com.exalt.it.belair.domain.order.exceptions.InvalidItemTypeException;
import com.exalt.it.belair.domain.order.exceptions.InvalidQuantityException;
import com.exalt.it.belair.domain.order.exceptions.InvalidSubtypeException;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.in.PlaceOrderUseCasePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceOrderControllerTest {

    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @Mock
    private PlaceOrderUseCasePort placeOrderUseCase;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        PlaceOrderController controller = new PlaceOrderController(placeOrderUseCase);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private Order createMockOrder(String... itemSpecs) {
        Order mockOrder = mock(Order.class);
        List<OrderItem> items = new java.util.ArrayList<>();
        int drinkTokenCost = 0;
        int snackTokenCost = 0;
        
        for (String spec : itemSpecs) {
            // Format: "DRINK,NORMAL_ALCOHOLIC,1"
            String[] parts = spec.split(",");
            String itemType = parts[0];
            String itemSubtype = parts[1];
            int quantity = Integer.parseInt(parts[2]);
            
            OrderItem mockItem = mock(OrderItem.class);
            when(mockItem.getItemType()).thenReturn(itemType);
            when(mockItem.getItemSubtype()).thenReturn(itemSubtype);
            when(mockItem.getQuantity()).thenReturn(quantity);
            items.add(mockItem);
            
            // Calculate token costs based on item type and subtype
            if ("DRINK".equals(itemType)) {
                switch (itemSubtype) {
                    case "NON_ALCOHOLIC":
                        drinkTokenCost += 0;
                        break;
                    case "NORMAL_ALCOHOLIC":
                        drinkTokenCost += quantity;
                        break;
                    case "PREMIUM_ALCOHOLIC":
                        drinkTokenCost += quantity * 2;
                        break;
                }
            } else if ("FOOD".equals(itemType)) {
                switch (itemSubtype) {
                    case "SNACK":
                        snackTokenCost += quantity;
                        break;
                    case "MEAL":
                        snackTokenCost += quantity * 3;
                        break;
                }
            }
        }
        when(mockOrder.getOrderId()).thenReturn("order-001");
        when(mockOrder.getFestivalGoerId()).thenReturn("fgv-001");
        when(mockOrder.getStatus()).thenReturn(OrderStatusEnum.PENDING);
        when(mockOrder.getItems()).thenReturn(items);
        when(mockOrder.getDrinkTokenCost()).thenReturn(drinkTokenCost);
        when(mockOrder.getSnackTokenCost()).thenReturn(snackTokenCost);
        return mockOrder;
    }

    @Test
    void post_shouldReturn201WithOrderDetails_whenPlacingOrderWithSingleDrink() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "NORMAL_ALCOHOLIC", 1))
        );
        Order mockOrder = mock(Order.class);
        OrderItem mockItem = mock(OrderItem.class);
        when(mockItem.getItemType()).thenReturn("DRINK");
        when(mockItem.getItemSubtype()).thenReturn("NORMAL_ALCOHOLIC");
        when(mockItem.getQuantity()).thenReturn(1);
        when(mockOrder.getOrderId()).thenReturn("order-001");
        when(mockOrder.getFestivalGoerId()).thenReturn("fgv-001");
        when(mockOrder.getStatus()).thenReturn(OrderStatusEnum.PENDING);
        when(mockOrder.getItems()).thenReturn(List.of(mockItem));
        when(mockOrder.getDrinkTokenCost()).thenReturn(1);
        when(mockOrder.getSnackTokenCost()).thenReturn(0);
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-001"))
                .andExpect(jsonPath("$.drinkTokensCost").value(1))
                .andExpect(jsonPath("$.snackTokensCost").value(0));
    }

    @Test
    void post_shouldReturn201WithDrinkTokensCostZero_whenPlacingOrderWithNonAlcoholicDrinks() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "NON_ALCOHOLIC", 3))
        );
        Order mockOrder = createMockOrder("DRINK,NON_ALCOHOLIC,3");
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drinkTokensCost").value(0));
    }

    @Test
    void post_shouldReturn201WithDrinkTokensCostFour_whenPlacingOrderWithPremiumDrinks() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "PREMIUM_ALCOHOLIC", 2))
        );
        Order mockOrder = createMockOrder("DRINK,PREMIUM_ALCOHOLIC,2");
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drinkTokensCost").value(4));
    }

    @Test
    void post_shouldReturn201WithSnackTokensCostTwo_whenPlacingOrderWithSnacks() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("FOOD", "SNACK", 2))
        );
        Order mockOrder = createMockOrder("FOOD,SNACK,2");
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snackTokensCost").value(2));
    }

    @Test
    void post_shouldReturn201WithSnackTokensCostSix_whenPlacingOrderWithMeals() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("FOOD", "MEAL", 2))
        );
        Order mockOrder = createMockOrder("FOOD,MEAL,2");
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snackTokensCost").value(6));
    }

    @Test
    void post_shouldReturn201WithComplexMixedOrder_whenPlacingMultipleItemTypes() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(
                        new OrderItemRequest("DRINK", "NORMAL_ALCOHOLIC", 2),
                        new OrderItemRequest("DRINK", "NON_ALCOHOLIC", 1),
                        new OrderItemRequest("FOOD", "SNACK", 2),
                        new OrderItemRequest("FOOD", "MEAL", 1)
                )
        );
        Order mockOrder = createMockOrder(
                "DRINK,NORMAL_ALCOHOLIC,2",
                "DRINK,NON_ALCOHOLIC,1",
                "FOOD,SNACK,2",
                "FOOD,MEAL,1"
        );
        when(placeOrderUseCase.placeOrder(any())).thenReturn(mockOrder);

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drinkTokensCost").value(2))
                .andExpect(jsonPath("$.snackTokensCost").value(5));
    }

    @Test
    void post_shouldReturn400_whenOrderIsEmpty() throws Exception {
        var request = new PlaceOrderRequest("fgv-001", List.of());
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new EmptyOrderException("Order items cannot be empty"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn400_whenItemTypeIsInvalid() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("INVALID", "SOMETHING", 1))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InvalidItemTypeException("Invalid item type: INVALID"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn400_whenSubtypeIsInvalidForItemType() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "SNACK", 1))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InvalidSubtypeException("Invalid subtype SNACK for item type DRINK"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn400_whenQuantityIsZero() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "NORMAL_ALCOHOLIC", 0))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InvalidQuantityException("Quantity must be greater than 0"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn400_whenQuantityIsNegative() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "NORMAL_ALCOHOLIC", -1))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InvalidQuantityException("Quantity must be greater than 0"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_shouldReturn404_whenFestivalGoerNotFound() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-999",
                List.of(new OrderItemRequest("DRINK", "NON_ALCOHOLIC", 1))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new FestivalGoerNotFoundException("Festival goer fgv-999 not found"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_shouldReturn422_whenInsufficientDrinkTokens() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("DRINK", "NORMAL_ALCOHOLIC", 3))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InsufficientTokensException("Insufficient drink tokens"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_shouldReturn422_whenInsufficientSnackTokens() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(new OrderItemRequest("FOOD", "MEAL", 3))
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InsufficientTokensException("Insufficient snack tokens"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_shouldReturn422_whenInsufficientBothTokenTypes() throws Exception {
        var request = new PlaceOrderRequest(
                "fgv-001",
                List.of(
                        new OrderItemRequest("DRINK", "PREMIUM_ALCOHOLIC", 1),
                        new OrderItemRequest("FOOD", "MEAL", 1)
                )
        );
        when(placeOrderUseCase.placeOrder(any())).thenThrow(new InsufficientTokensException("Insufficient tokens"));

        mvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }


}
