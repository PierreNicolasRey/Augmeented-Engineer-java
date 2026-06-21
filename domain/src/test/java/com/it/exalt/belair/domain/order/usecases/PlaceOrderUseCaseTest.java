package com.it.exalt.belair.domain.order.usecases;

import com.it.exalt.belair.domain.order.model.Order;
import com.it.exalt.belair.domain.order.model.OrderItem;
import com.it.exalt.belair.domain.order.model.DrinkType;
import com.it.exalt.belair.domain.order.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderUseCaseTest {
    
    private PlaceOrderUseCase sut;  // System Under Test
    
    @Test
    void placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNormalAlcoholicDrink() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        int drinkTokens = 6;
        int snackTokens = 9;
        
        // WHEN placing an order with 1 normal alcoholic drink
        sut = new PlaceOrderUseCase();
        var item = OrderItem.createDrinkItem(DrinkType.NORMAL_ALCOHOLIC, 1);
        Order order = sut.placeOrder(festivalGoerId, item, drinkTokens, snackTokens);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        // And the order contains 1 drink item
        assertThat(order.getItemCount()).isEqualTo(1);
        // And the total drink token cost is 1
        assertThat(order.getDrinkTokenCost()).isEqualTo(1);
    }
}
