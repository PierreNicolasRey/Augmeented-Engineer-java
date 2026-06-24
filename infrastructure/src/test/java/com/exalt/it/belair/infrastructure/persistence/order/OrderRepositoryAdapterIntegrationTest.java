package com.exalt.it.belair.infrastructure.persistence.order;

import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FoodTypeEnum;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepositoryAdapter.
 * Tests the persistence layer (Infrastructure) adapter that bridges Domain and JPA.
 * 
 * Tests verify that Orders can be saved to and retrieved from the database.
 */
class OrderRepositoryAdapterIntegrationTest {
    
    private OrderRepositoryAdapter adapter;
    
    @BeforeEach
    void setUp() {
        adapter = new OrderRepositoryAdapter();
    }
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly_whenOrderHasSingleDrinkItem() {
        // GIVEN an Order for festival goer "fgv-001"
        String festivalGoerId = "fgv-001";
        
        // And the order contains 1 normal alcoholic drink
        OrderItem item = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 1);
        List<OrderItem> items = List.of(item);
        Order order = new Order("order-001", festivalGoerId, items, OrderStatusEnum.PENDING);
        
        // WHEN the order is saved to the database
        Order savedOrder = adapter.save(order);
        
        // THEN the order can be retrieved by ID
        Optional<Order> retrievedOrder = adapter.findById("order-001");
        assertThat(retrievedOrder).isPresent();
        
        // And the order contains 1 item
        assertThat(retrievedOrder.get().getItems()).hasSize(1);
        
        // And the item cost is 1 drink token
        assertThat(retrievedOrder.get().getDrinkTokenCost()).isEqualTo(1);
        
        // And the order status is "PENDING"
        assertThat(retrievedOrder.get().getStatus()).isEqualTo(OrderStatusEnum.PENDING);
    }
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly_whenOrderHasMultipleDrinkItems() {
        // GIVEN an Order for festival goer "fgv-001"
        String festivalGoerId = "fgv-001";
        
        // And the order contains 2 normal alcoholic drinks and 1 non-alcoholic drink
        OrderItem normalAlcoholic1 = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 2);
        OrderItem nonAlcoholic = OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 1);
        List<OrderItem> items = List.of(normalAlcoholic1, nonAlcoholic);
        Order order = new Order("order-002", festivalGoerId, items, OrderStatusEnum.PENDING);
        
        // WHEN the order is saved to the database
        Order savedOrder = adapter.save(order);
        
        // THEN the order can be retrieved by ID
        Optional<Order> retrievedOrder = adapter.findById("order-002");
        assertThat(retrievedOrder).isPresent();
        
        // And the order contains 2 OrderItem objects (qty 2 + qty 1 = 3 individual items)
        assertThat(retrievedOrder.get().getItems()).hasSize(2);
        
        // And the order cost is 2 drink tokens
        assertThat(retrievedOrder.get().getDrinkTokenCost()).isEqualTo(2);
        
        // And items are in the correct order
        List<OrderItem> retrievedItems = retrievedOrder.get().getItems();
        assertThat(retrievedItems.get(0).getItemSubtype()).isEqualTo("NORMAL_ALCOHOLIC");
        assertThat(retrievedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(retrievedItems.get(1).getItemSubtype()).isEqualTo("NON_ALCOHOLIC");
        assertThat(retrievedItems.get(1).getQuantity()).isEqualTo(1);
    }
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly_whenOrderHasFoodItems() {
        // GIVEN an Order for festival goer "fgv-001"
        String festivalGoerId = "fgv-001";
        
        // And the order contains 2 snacks and 1 meal
        OrderItem snacks = OrderItem.createFoodItem(FoodTypeEnum.SNACK, 2);
        OrderItem meal = OrderItem.createFoodItem(FoodTypeEnum.MEAL, 1);
        List<OrderItem> items = List.of(snacks, meal);
        Order order = new Order("order-003", festivalGoerId, items, OrderStatusEnum.PENDING);
        
        // WHEN the order is saved to the database
        Order savedOrder = adapter.save(order);
        
        // THEN the order can be retrieved by ID
        Optional<Order> retrievedOrder = adapter.findById("order-003");
        assertThat(retrievedOrder).isPresent();
        
        // And the order contains 2 OrderItem objects (qty 2 + qty 1 = 3 individual items)
        assertThat(retrievedOrder.get().getItems()).hasSize(2);
        
        // And the order cost is 5 snack tokens (2 snacks @ 1 token + 1 meal @ 3 tokens)
        assertThat(retrievedOrder.get().getSnackTokenCost()).isEqualTo(5);
    }
    
    @Test
    void save_shouldPersistOrderAndRetrieveItCorrectly_whenOrderIsComplexMixed() {
        // GIVEN an Order for festival goer "fgv-001"
        String festivalGoerId = "fgv-001";
        
        // And the order contains 2 normal alcoholic drinks, 1 non-alcoholic drink, 2 snacks, and 1 meal
        OrderItem normalAlcoholic = OrderItem.createDrinkItem(DrinkTypeEnum.NORMAL_ALCOHOLIC, 2);
        OrderItem nonAlcoholic = OrderItem.createDrinkItem(DrinkTypeEnum.NON_ALCOHOLIC, 1);
        OrderItem snacks = OrderItem.createFoodItem(FoodTypeEnum.SNACK, 2);
        OrderItem meal = OrderItem.createFoodItem(FoodTypeEnum.MEAL, 1);
        List<OrderItem> items = List.of(normalAlcoholic, nonAlcoholic, snacks, meal);
        Order order = new Order("order-004", festivalGoerId, items, OrderStatusEnum.PENDING);
        
        // WHEN the order is saved to the database
        Order savedOrder = adapter.save(order);
        
        // THEN the order can be retrieved by ID
        Optional<Order> retrievedOrder = adapter.findById("order-004");
        assertThat(retrievedOrder).isPresent();
        
        // And the order contains 4 OrderItem objects (qty 2 + qty 1 + qty 2 + qty 1 = 6 individual items)
        assertThat(retrievedOrder.get().getItems()).hasSize(4);
        
        // And the order drink token cost is 2 (2 normal alcoholic @ 1 token each)
        assertThat(retrievedOrder.get().getDrinkTokenCost()).isEqualTo(2);
        
        // And the order snack token cost is 5 (2 snacks @ 1 token + 1 meal @ 3 tokens)
        assertThat(retrievedOrder.get().getSnackTokenCost()).isEqualTo(5);
    }
    
    @Test
    void findById_shouldReturnEmpty_whenOrderDoesNotExist() {
        // GIVEN no order with ID "ord-999" exists
        String nonExistentOrderId = "ord-999";
        
        // WHEN retrieving order by ID "ord-999"
        Optional<Order> retrievedOrder = adapter.findById(nonExistentOrderId);
        
        // THEN no order is returned (Optional.empty())
        assertThat(retrievedOrder).isEmpty();
    }
}
