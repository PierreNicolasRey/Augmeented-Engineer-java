package com.exalt.it.belair.domain.order.services;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FoodTypeEnum;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates estimated order readiness time based on ordered items.
 */
public class EstimatedTimeCalculator {
    private static final String ITEM_TYPE_DRINK = "DRINK";
    private static final String ITEM_TYPE_FOOD = "FOOD";

    public int calculateEstimatedTimeMinutes(Order order) {
        int nonAlcoholicMinutes = 0;
        int normalAlcoholicMinutes = 0;
        int premiumAlcoholicMinutes = 0;
        int snackMinutes = 0;
        int mealMinutes = calculateMealMinutes(order);

        for (OrderItem item : order.getItems()) {
            if (ITEM_TYPE_DRINK.equals(item.getItemType())) {
                DrinkTypeEnum drinkType = resolveDrinkType(item);
                if (drinkType == null) {
                    continue;
                }
                switch (drinkType) {
                    case NON_ALCOHOLIC -> nonAlcoholicMinutes += item.getQuantity();
                    case NORMAL_ALCOHOLIC -> normalAlcoholicMinutes += item.getQuantity() * 2;
                    case PREMIUM_ALCOHOLIC -> premiumAlcoholicMinutes += item.getQuantity() * 3;
                }
            } else if (ITEM_TYPE_FOOD.equals(item.getItemType()) && FoodTypeEnum.SNACK.name().equals(item.getItemSubtype())) {
                snackMinutes += item.getQuantity() * 2;
            }
        }

        int longestDrinkMinutes = Math.max(nonAlcoholicMinutes, Math.max(normalAlcoholicMinutes, premiumAlcoholicMinutes));

        if (mealMinutes > 0) {
            return mealMinutes + longestDrinkMinutes + snackMinutes;
        }

        int totalDrinkMinutes = nonAlcoholicMinutes + normalAlcoholicMinutes + premiumAlcoholicMinutes;
        return totalDrinkMinutes + snackMinutes;
    }

    private int calculateMealMinutes(Order order) {
        Set<String> nonNullMealIds = order.getItems().stream()
                .filter(item -> ITEM_TYPE_FOOD.equals(item.getItemType()))
                .filter(item -> FoodTypeEnum.MEAL.name().equals(item.getItemSubtype()))
                .map(OrderItem::getItemId)
                .filter(itemId -> itemId != null)
                .collect(Collectors.toSet());

        boolean hasGenericMealType = order.getItems().stream()
                .filter(item -> ITEM_TYPE_FOOD.equals(item.getItemType()))
                .filter(item -> FoodTypeEnum.MEAL.name().equals(item.getItemSubtype()))
                .anyMatch(item -> item.getItemId() == null);

        int distinctMealTypes = nonNullMealIds.size() + (hasGenericMealType ? 1 : 0);
        return distinctMealTypes * 10;
    }

    private DrinkTypeEnum resolveDrinkType(OrderItem item) {
        try {
            return DrinkTypeEnum.valueOf(item.getItemSubtype());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
