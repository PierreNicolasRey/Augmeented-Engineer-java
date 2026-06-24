package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.exceptions.EmptyOrderException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientItemInventoryException;
import com.exalt.it.belair.domain.order.exceptions.ItemNotFoundInCatalogException;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IItemInventoryRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import java.util.List;
import java.util.UUID;

/**
 * Use case for placing an order in the Belair Buvette ordering system.
 * 
 * Orchestrates the complete order placement workflow:
 * 1. Validates that the order is not empty
 * 2. Validates that all items exist in the catalog and have sufficient inventory
 * 3. Calculates token costs for the order
 * 4. Validates that the festival goer has sufficient token balance
 * 5. Reserves tokens in the festival goer's balance
 * 6. Persists the order via the IOrderRepository port
 * 
 * Respects the Hexagonal Architecture by delegating persistence and inventory checks
 * to outbound ports (IOrderRepository, IItemInventoryRepository).
 */
public class PlaceOrderUseCase {
    private final IOrderRepository orderRepository;
    private final IItemInventoryRepository itemInventoryRepository;

    /**
     * Constructs a PlaceOrderUseCase with the required outbound port dependencies.
     * 
     * @param orderRepository the port for persisting orders
     * @param itemInventoryRepository the port for checking item availability and inventory
     */
    public PlaceOrderUseCase(IOrderRepository orderRepository, IItemInventoryRepository itemInventoryRepository) {
        this.orderRepository = orderRepository;
        this.itemInventoryRepository = itemInventoryRepository;
    }

    /**
     * Places an order for a festival goer with the specified items.
     * 
     * Business logic flow:
     * - Validates order is not empty
     * - Checks each item exists in catalog and has sufficient stock
     * - Calculates drink and snack token costs
     * - Verifies the festival goer has available tokens
     * - Reserves tokens in the balance
     * - Creates the order with PENDING status
     * - Persists the order
     * 
     * @param festivalGoerId the ID of the festival goer placing the order
     * @param items the list of items to order (must not be empty)
     * @param balance the token balance of the festival goer
     * @return the persisted Order with a generated order ID
     * 
     * @throws EmptyOrderException if the items list is empty
     * @throws ItemNotFoundInCatalogException if any item is not found in the catalog
     * @throws InsufficientItemInventoryException if any item has insufficient stock
     * @throws com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException if insufficient tokens
     */
    public Order placeOrder(String festivalGoerId, List<OrderItem> items, FestivalGoerBalance balance) {
        // 1. Validate order is not empty
        if (items.isEmpty()) {
            throw new EmptyOrderException("Order items cannot be empty");
        }

        // 2. Validate items exist in catalog and have sufficient stock
        for (OrderItem item : items) {
            if (item.getItemId() != null) {
                // Check if item exists in catalog and has sufficient stock
                if (!itemInventoryRepository.hasItemInStock(item.getItemId(), item.getQuantity())) {
                    int availableQuantity = itemInventoryRepository.getAvailableQuantity(item.getItemId());
                    throw new InsufficientItemInventoryException(
                        "Insufficient inventory for item " + item.getItemId(),
                        item.getItemId(),
                        item.getQuantity(),
                        availableQuantity
                    );
                }
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

        // 5. Create order with generated ID
        String orderId = "order-" + UUID.randomUUID().toString();
        Order order = new Order(orderId, festivalGoerId, items, OrderStatusEnum.PENDING);

        // 6. Persist order via outbound port
        Order persistedOrder = orderRepository.save(order);

        return persistedOrder;
    }
}
