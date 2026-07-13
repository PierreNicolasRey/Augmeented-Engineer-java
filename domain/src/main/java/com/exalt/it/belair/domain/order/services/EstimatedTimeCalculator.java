package com.exalt.it.belair.domain.order.services;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates estimated order readiness time based on ordered items.
 */
public class EstimatedTimeCalculator {
    private static final String ITEM_TYPE_DRINK = "DRINK";
    private static final String ITEM_TYPE_FOOD = "FOOD";
    private static final String DRINK_SUBTYPE_NON_ALCOHOLIC = "NON_ALCOHOLIC";
    private static final String DRINK_SUBTYPE_NORMAL_ALCOHOLIC = "NORMAL_ALCOHOLIC";
    private static final String DRINK_SUBTYPE_PREMIUM_ALCOHOLIC = "PREMIUM_ALCOHOLIC";
    private static final String FOOD_SUBTYPE_SNACK = "SNACK";
    private static final String FOOD_SUBTYPE_MEAL = "MEAL";

    public int calculateEstimatedTimeMinutes(Order order) {
        int nonAlcoholicMinutes = 0;
        int normalAlcoholicMinutes = 0;
        int premiumAlcoholicMinutes = 0;
        int snackMinutes = 0;
        int mealMinutes = calculateMealMinutes(order);

        for (OrderItem item : order.getItems()) {
            if (ITEM_TYPE_DRINK.equals(item.getItemType())) {
                if (DRINK_SUBTYPE_NON_ALCOHOLIC.equals(item.getItemSubtype())) {
                    nonAlcoholicMinutes += item.getQuantity();
                } else if (DRINK_SUBTYPE_NORMAL_ALCOHOLIC.equals(item.getItemSubtype())) {
                    normalAlcoholicMinutes += item.getQuantity() * 2;
                } else if (DRINK_SUBTYPE_PREMIUM_ALCOHOLIC.equals(item.getItemSubtype())) {
                    premiumAlcoholicMinutes += item.getQuantity() * 3;
                }
            } else if (ITEM_TYPE_FOOD.equals(item.getItemType()) && FOOD_SUBTYPE_SNACK.equals(item.getItemSubtype())) {
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
                .filter(item -> FOOD_SUBTYPE_MEAL.equals(item.getItemSubtype()))
                .map(OrderItem::getItemId)
                .filter(itemId -> itemId != null)
                .collect(Collectors.toSet());

        boolean hasGenericMealType = order.getItems().stream()
                .filter(item -> ITEM_TYPE_FOOD.equals(item.getItemType()))
                .filter(item -> FOOD_SUBTYPE_MEAL.equals(item.getItemSubtype()))
                .anyMatch(item -> item.getItemId() == null);

        int distinctMealTypes = nonNullMealIds.size() + (hasGenericMealType ? 1 : 0);
        return distinctMealTypes * 10;
    }
}
