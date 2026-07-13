package com.exalt.it.belair.domain.order.services;

import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;

/**
 * Calculates estimated order readiness time based on ordered items.
 */
public class EstimatedTimeCalculator {

    public int calculateEstimatedTimeMinutes(Order order) {
        int nonAlcoholicMinutes = 0;
        int normalAlcoholicMinutes = 0;
        int premiumAlcoholicMinutes = 0;
        int snackMinutes = 0;
        int mealMinutes = calculateMealMinutes(order);

        for (OrderItem item : order.getItems()) {
            if ("DRINK".equals(item.getItemType())) {
                if ("NON_ALCOHOLIC".equals(item.getItemSubtype())) {
                    nonAlcoholicMinutes += item.getQuantity();
                } else if ("NORMAL_ALCOHOLIC".equals(item.getItemSubtype())) {
                    normalAlcoholicMinutes += item.getQuantity() * 2;
                } else if ("PREMIUM_ALCOHOLIC".equals(item.getItemSubtype())) {
                    premiumAlcoholicMinutes += item.getQuantity() * 3;
                }
            } else if ("FOOD".equals(item.getItemType()) && "SNACK".equals(item.getItemSubtype())) {
                snackMinutes += item.getQuantity() * 2;
            }
        }

        int totalDrinkMinutes = nonAlcoholicMinutes + normalAlcoholicMinutes + premiumAlcoholicMinutes;
        int longestDrinkMinutes = Math.max(nonAlcoholicMinutes, Math.max(normalAlcoholicMinutes, premiumAlcoholicMinutes));

        if (mealMinutes > 0) {
            return mealMinutes + longestDrinkMinutes + snackMinutes;
        }

        return totalDrinkMinutes + snackMinutes;
    }

    private int calculateMealMinutes(Order order) {
        long distinctMealTypes = order.getItems().stream()
                .filter(item -> "FOOD".equals(item.getItemType()))
                .filter(item -> "MEAL".equals(item.getItemSubtype()))
                .map(item -> item.getItemId() == null ? "MEAL" : item.getItemId())
                .distinct()
                .count();
        return (int) distinctMealTypes * 10;
    }
}
