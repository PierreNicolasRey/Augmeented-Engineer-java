package com.exalt.it.belair.domain.order.services;

import com.exalt.it.belair.domain.order.exceptions.InvalidSubtypeException;
import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FoodTypeEnum;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstimatedTimeCalculatorTest {
    private final EstimatedTimeCalculator sut = new EstimatedTimeCalculator();

    @Test
    void calculateEstimatedTimeMinutes_shouldReturnOneMinutePerNonAlcoholicItem() {
        Order order = createOrder(List.of(OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 5)));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(5);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldReturnTwoMinutesPerNormalAlcoholicItem() {
        Order order = createOrder(List.of(OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 3)));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(6);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldReturnThreeMinutesPerPremiumAlcoholicItem() {
        Order order = createOrder(List.of(OrderItem.createDrinkItem(DrinkTypeEnum.PREMIUM_ALCOHOLIC, 2)));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(6);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldCountMixedDrinksPerItem() {
        Order order = createOrder(List.of(
                OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 5),
                OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 1)
        ));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(7);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldReturnTwoMinutesPerSnackItem() {
        Order order = createOrder(List.of(
                OrderItem.createFoodItem(FoodTypeEnum.SNACK, 2),
                OrderItem.createFoodItem(FoodTypeEnum.SNACK, 3)
        ));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(10);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldCountMealsByDistinctType() {
        Order order = createOrder(List.of(
                new OrderItem("meal-burger", "FOOD", "MEAL", 2),
                new OrderItem("meal-burger", "FOOD", "MEAL", 1),
                new OrderItem("meal-pasta", "FOOD", "MEAL", 3)
        ));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(20);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldAddMealsAndLongestDrinkAndSnacks() {
        Order order = createOrder(List.of(
                new OrderItem("meal-burger", "FOOD", "MEAL", 1),
                OrderItem.createDrinkItem(DrinkTypeEnum.PREMIUM_ALCOHOLIC, 1),
                OrderItem.createFoodItem(FoodTypeEnum.SNACK, 2)
        ));

        assertThat(sut.calculateEstimatedTimeMinutes(order)).isEqualTo(17);
    }

    @Test
    void calculateEstimatedTimeMinutes_shouldThrowInvalidSubtypeExceptionForUnknownDrinkSubtype() {
        Order order = createOrder(List.of(new OrderItem("drink-1", "DRINK", "UNKNOWN_SUBTYPE", 1)));

        assertThatThrownBy(() -> sut.calculateEstimatedTimeMinutes(order))
                .isInstanceOf(InvalidSubtypeException.class)
                .hasMessage("Invalid drink subtype: UNKNOWN_SUBTYPE");
    }

    private Order createOrder(List<OrderItem> items) {
        return new Order("ord-1", "fgv-1", items, OrderStatusEnum.PENDING);
    }
}
