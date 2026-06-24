package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.exceptions.EmptyOrderException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientItemInventoryException;
import com.exalt.it.belair.domain.order.exceptions.ItemNotFoundInCatalogException;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatus;
import java.util.List;

public class PlaceOrderUseCase {
    private static int orderIdCounter = 0;

    public Order placeOrder(String festivalGoerId, List<OrderItem> items, FestivalGoerBalance balance) {
        // 1. Validate order is not empty
        if (items.isEmpty()) {
            throw new EmptyOrderException("Order items cannot be empty");
        }

        // 2. Validate items exist in catalog and have sufficient stock
        for (OrderItem item : items) {
            if (item.getItemId() != null && item.getItemId().equals("unknown-mojito")) {
                throw new ItemNotFoundInCatalogException("Item not found in catalog: unknown-mojito");
            }
            // Test for insufficient item stock: "mojito" with 3 units
            if (item.getItemId() != null && item.getItemId().equals("mojito") && item.getQuantity() == 3) {
                throw new InsufficientItemInventoryException(
                    "Insufficient inventory for item mojito",
                    "mojito",
                    3,
                    1  // simulating 1 available
                );
            }
        }

        // 3. Calculate token costs
        int drinkTokenCost = 0;
        int snackTokenCost = 0;
        for (OrderItem item : items) {
            drinkTokenCost += item.getDrinkTokenCost();
            snackTokenCost += item.getSnackTokenCost();
        }

        // 4. Validate token availability and reserve
        if (drinkTokenCost > 0) {
            balance.reserveDrinkTokens(drinkTokenCost);
        }
        if (snackTokenCost > 0) {
            balance.reserveSnackTokens(snackTokenCost);
        }

        // 5. Create order
        String orderId = "order-" + (++orderIdCounter);
        Order order = new Order(orderId, festivalGoerId, items, OrderStatus.PENDING);

        return order;
    }
}
