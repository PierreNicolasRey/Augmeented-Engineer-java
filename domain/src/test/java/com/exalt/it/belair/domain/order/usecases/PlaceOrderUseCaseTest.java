package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.exceptions.EmptyOrderException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientItemInventoryException;
import com.exalt.it.belair.domain.order.exceptions.InsufficientTokensException;
import com.exalt.it.belair.domain.order.exceptions.ItemNotFoundInCatalogException;
import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.FoodTypeEnum;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IItemInventoryRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceOrderUseCaseTest {
    
    private PlaceOrderUseCase sut;
    private TestOrderRepository orderRepository;
    private TestItemInventoryRepository itemInventoryRepository;
    
    @BeforeEach
    void setUp() {
        orderRepository = new TestOrderRepository();
        itemInventoryRepository = new TestItemInventoryRepository();
        sut = new PlaceOrderUseCase(orderRepository, itemInventoryRepository);
    }
    
    // ============ HAPPY PATH: Single Item Scenarios ============
    
    @Test
    void placeOrder_shouldCreateOrderWithPendingStatus_whenPlacingOrderWithSingleNonAlcoholicDrink() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 1 non-alcoholic drink
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 1);
        Order order = sut.placeOrder(festivalGoerId, List.of(item), balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 1 drink item
        assertThat(order.getItems()).hasSize(1);
        // And the total drink token cost is 0
        assertThat(order.getDrinkTokenCost()).isEqualTo(0);
        // And the festival goer's token balance is not changed
        assertThat(balance.getDrinkTokens()).isEqualTo(6);
        assertThat(balance.getSnackTokens()).isEqualTo(9);
    }
    
    @Test
    void placeOrder_shouldReserveTokens_whenPlacingOrderWithSingleNormalAlcoholicDrink() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 1 normal alcoholic drink
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 1);
        Order order = sut.placeOrder(festivalGoerId, List.of(item), balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 1 drink item
        assertThat(order.getItems()).hasSize(1);
        // And the total drink token cost is 1
        assertThat(order.getDrinkTokenCost()).isEqualTo(1);
        // And the festival goer's drink tokens are reserved (1 reserved, 5 available)
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(1);
        assertThat(balance.getAvailableDrinkTokens()).isEqualTo(5);
    }
    
    @Test
    void placeOrder_shouldReserveCorrectCost_whenPlacingOrderWithPremiumAlcoholicDrinks() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 2 premium alcoholic drinks
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.PREMIUM_ALCOHOLIC, 2);
        Order order = sut.placeOrder(festivalGoerId, List.of(item), balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 2 drink items
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
        // And the total drink token cost is 4
        assertThat(order.getDrinkTokenCost()).isEqualTo(4);
        // And the festival goer's drink tokens are reserved (4 reserved, 2 available)
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(4);
        assertThat(balance.getAvailableDrinkTokens()).isEqualTo(2);
    }
    
    @Test
    void placeOrder_shouldReserveSnackTokens_whenPlacingOrderWithSnacks() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 3 snacks
        OrderItem item = OrderItem.createFoodItem(FoodTypeEnum.SNACK, 3);
        Order order = sut.placeOrder(festivalGoerId, List.of(item), balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 3 food items
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(3);
        // And the total snack token cost is 3
        assertThat(order.getSnackTokenCost()).isEqualTo(3);
        // And the festival goer's snack tokens are reserved (3 reserved, 6 available)
        assertThat(balance.getReservedSnackTokens()).isEqualTo(3);
        assertThat(balance.getAvailableSnackTokens()).isEqualTo(6);
    }
    
    @Test
    void placeOrder_shouldReserveCorrectCost_whenPlacingOrderWithMeals() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 2 meals
        OrderItem item = OrderItem.createFoodItem(FoodTypeEnum.MEAL, 2);
        Order order = sut.placeOrder(festivalGoerId, List.of(item), balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 2 food items
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
        // And the total snack token cost is 6 (2 meals * 3 tokens per meal)
        assertThat(order.getSnackTokenCost()).isEqualTo(6);
        // And the festival goer's snack tokens are reserved (6 reserved, 3 available)
        assertThat(balance.getReservedSnackTokens()).isEqualTo(6);
        assertThat(balance.getAvailableSnackTokens()).isEqualTo(3);
    }
    
    // ============ HAPPY PATH: Complex Mixed Order Scenarios ============
    
    @Test
    void placeOrder_shouldReserveCorrectCosts_whenPlacingMixedOrder() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with:
        // - 2 normal alcoholic drinks (cost: 2 drink tokens)
        // - 1 non-alcoholic drink (cost: 0 drink tokens)
        // - 2 snacks (cost: 2 snack tokens)
        // - 1 meal (cost: 3 snack tokens)
        List<OrderItem> items = List.of(
            OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 2),
            OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 1),
            OrderItem.createFoodItem(FoodTypeEnum.SNACK, 2),
            OrderItem.createFoodItem(FoodTypeEnum.MEAL, 1)
        );
        Order order = sut.placeOrder(festivalGoerId, items, balance);
        
        // THEN an Order is created with status PENDING
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatusEnum.PENDING);
        // And the order contains 4 items
        assertThat(order.getItems()).hasSize(4);
        // And the total drink token cost is 2
        assertThat(order.getDrinkTokenCost()).isEqualTo(2);
        // And the total snack token cost is 5 (2 + 3)
        assertThat(order.getSnackTokenCost()).isEqualTo(5);
        // And the festival goer's drink tokens are reserved (2 reserved, 4 available)
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(2);
        assertThat(balance.getAvailableDrinkTokens()).isEqualTo(4);
        // And the festival goer's snack tokens are reserved (5 reserved, 4 available)
        assertThat(balance.getReservedSnackTokens()).isEqualTo(5);
        assertThat(balance.getAvailableSnackTokens()).isEqualTo(4);
    }
    
    // ============ ERROR CASES: Insufficient Tokens ============
    
    @Test
    void placeOrder_shouldThrowException_whenInsufficientDrinkTokens() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 1 drink token and 9 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(1, 9);
        
        // WHEN placing an order with 3 normal alcoholic drinks (cost: 3 tokens, but only 1 available)
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 3);
        
        // THEN an InsufficientTokensException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(item), balance))
            .isInstanceOf(InsufficientTokensException.class);
        // And no Order is created / balance is unchanged
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(0);
    }
    
    @Test
    void placeOrder_shouldThrowException_whenInsufficientSnackTokens() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 6 drink tokens and 2 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 2);
        
        // WHEN placing an order with 3 meals (cost: 9 snack tokens, but only 2 available)
        OrderItem item = OrderItem.createFoodItem(FoodTypeEnum.MEAL, 3);
        
        // THEN an InsufficientTokensException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(item), balance))
            .isInstanceOf(InsufficientTokensException.class);
        // And the festival goer's balance is unchanged
        assertThat(balance.getReservedSnackTokens()).isEqualTo(0);
    }
    
    @Test
    void placeOrder_shouldThrowException_whenInsufficientBothTokenTypes() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        // And the festival goer has 0 drink tokens and 0 snack tokens
        FestivalGoerBalance balance = new FestivalGoerBalance(0, 0);
        
        // WHEN placing an order with any items
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.PREMIUM_ALCOHOLIC, 1);
        
        // THEN an InsufficientTokensException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(item), balance))
            .isInstanceOf(InsufficientTokensException.class);
    }
    
    // ============ ERROR CASES: Empty Order ============
    
    @Test
    void placeOrder_shouldThrowException_whenOrderIsEmpty() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 0 items
        // THEN an EmptyOrderException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(), balance))
            .isInstanceOf(EmptyOrderException.class);
    }
    
    // ============ ERROR CASES: Item Inventory Validation ============
    
    @Test
    void placeOrder_shouldThrowException_whenItemNotFoundInCatalog() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        // And the catalog does not contain item "unknown-mojito"
        
        // WHEN placing an order for an item that doesn't exist in inventory
        OrderItem item = new OrderItem("unknown-mojito", "DRINK", "NORMAL_ALCOHOLIC", 2);
        
        // THEN an ItemNotFoundInCatalogException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(item), balance))
            .isInstanceOf(ItemNotFoundInCatalogException.class);
        // And no Order is created / balance is unchanged
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(0);
        assertThat(balance.getReservedSnackTokens()).isEqualTo(0);
    }
    
    @Test
    void placeOrder_shouldThrowException_whenInsufficientItemStock() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        // And the following item is in inventory with limited stock
        // (stock will be provided via ItemAvailabilityService)
        
        // WHEN placing an order for more items than available in stock
        OrderItem item = new OrderItem("mojito", "DRINK", "NORMAL_ALCOHOLIC", 3);
        
        // THEN an InsufficientItemInventoryException is raised
        assertThatThrownBy(() -> sut.placeOrder(festivalGoerId, List.of(item), balance))
            .isInstanceOf(InsufficientItemInventoryException.class);
        // And no Order is created / balance is unchanged
        assertThat(balance.getReservedDrinkTokens()).isEqualTo(0);
    }
    
    // ============ EVENT PUBLISHING ============
    
    @Test
    void placeOrder_shouldPublishOrderPlacedEvent_whenOrderIsSuccessful() {
        // GIVEN a festival goer with ID "fgv-001"
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        
        // WHEN placing an order with 2 normal alcoholic drinks and 1 snack
        List<OrderItem> items = List.of(
            OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 2),
            OrderItem.createFoodItem(FoodTypeEnum.SNACK, 1)
        );
        Order order = sut.placeOrder(festivalGoerId, items, balance);
        
        // THEN an OrderPlacedEvent is published with correct values
        // (Event verification would be done via EventCapture in GREEN phase)
        assertThat(order).isNotNull();
        assertThat(order.getDrinkTokenCost()).isEqualTo(2);
        assertThat(order.getSnackTokenCost()).isEqualTo(1);
    }

    // ============ TEST DOUBLES ============

    /**
     * Test double for IOrderRepository.
     * Simulates order persistence for testing.
     */
    static class TestOrderRepository implements IOrderRepository {
        @Override
        public Order save(Order order) {
            // In tests, simply return the order as-is
            // (In real implementation, this would persist to database)
            return order;
        }
    }

    /**
     * Test double for IItemInventoryRepository.
     * Simulates inventory checks for testing.
     * 
     * Rules:
     * - "unknown-mojito" is not found in catalog
     * - "mojito" with quantity 3 has insufficient stock (only 1 available)
     * - All other items are considered available
     */
    static class TestItemInventoryRepository implements IItemInventoryRepository {
        @Override
        public boolean hasItemInStock(String itemId, int requestedQuantity) {
            // Simulate item not found in catalog
            if ("unknown-mojito".equals(itemId)) {
                throw new ItemNotFoundInCatalogException("Item not found in catalog: " + itemId);
            }
            // Simulate insufficient stock for mojito with 3 units
            if ("mojito".equals(itemId) && requestedQuantity == 3) {
                return false;
            }
            // All other items are assumed to be in stock
            return true;
        }

        @Override
        public int getAvailableQuantity(String itemId) {
            // Simulate available quantities for test scenarios
            if ("mojito".equals(itemId)) {
                return 1; // Only 1 mojito available for testing insufficient stock
            }
            // Default: item not found or abundant stock
            return 0;
        }
    }
}
