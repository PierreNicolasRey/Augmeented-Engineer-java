package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeCancelledException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IFestivalGoerRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RED TDD Test for CancelOrderUseCase.
 * 
 * Scenario: Cancel pending order and unreserve tokens
 *   Given a festival goer "fgv-001" with 6 drink tokens total, 3 reserved, 3 available
 *   And an order "ord-001" with status "PENDING" costing 1 drink token
 *   And reserved tokens for this order: 1 drink, 0 snack
 *   When the cancel order use case is executed with orderId "ord-001"
 *   Then the order status is changed to "CANCELLED"
 *   And the unreserveTokens method is called with 1 drink, 0 snack
 *   And the token balance now shows: 6 total, 2 reserved (3 - 1), 4 available (6 - 2)
 *   And "updatedAt" timestamp is set
 */
class CancelOrderUseCaseTest {
    private CancelOrderUseCase sut;  // System Under Test
    private FakeFestivalGoerRepository festivalGoerRepository;
    private FakeOrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        festivalGoerRepository = new FakeFestivalGoerRepository();
        orderRepository = new FakeOrderRepository();
        sut = new CancelOrderUseCase(orderRepository, festivalGoerRepository);
    }

    @Test
    void cancelOrder_shouldChangeStatusToCANCELLED_andUnreserveTokens_whenOrderIsPending() {
        // GIVEN a festival goer "fgv-001" with 6 drink tokens total, 3 reserved, 3 available
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 0);  // 6 drink, 0 snack (total)
        balance.reserveDrinkTokens(3);  // Reserve 3 drink tokens (3 available)
        festivalGoerRepository.add(festivalGoerId, balance);

        // And an order "ord-001" with status "PENDING" costing 1 drink token
        String orderId = "ord-001";
        Order order = new Order(orderId, festivalGoerId, List.of(), OrderStatusEnum.PENDING);
        // And reserved tokens for this order: 1 drink, 0 snack
        order.setReservedTokens(1, 0);
        orderRepository.add(order);

        // WHEN the cancel order use case is executed with orderId "ord-001"
        sut.execute(orderId);

        // THEN the order status is changed to "CANCELLED"
        Order cancelledOrder = orderRepository.find(orderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatusEnum.CANCELLED);

        // And the unreserveTokens method is called with 1 drink, 0 snack
        // Verified implicitly by the balance calculation below

        // And the token balance now shows: 6 total, 2 reserved (3 - 1), 4 available (6 - 2)
        FestivalGoerBalance updatedBalance = festivalGoerRepository.findBalance(festivalGoerId);
        assertThat(updatedBalance.getDrinkTokens()).isEqualTo(6);  // Total unchanged
        assertThat(updatedBalance.getReservedDrinkTokens()).isEqualTo(2);  // 3 - 1 = 2
        assertThat(updatedBalance.getAvailableDrinkTokens()).isEqualTo(4);  // 6 - 2 = 4

        // And "updatedAt" timestamp is set
        assertThat(cancelledOrder.getUpdatedAt()).isNotNull();
    }

    // ============ INNER CLASSES: ALL PRODUCTION CODE BELOW ============

    /**
     * Fake implementation of the festival goer repository for testing.
     * Stores balance by festival goer ID in memory.
     */
    static class FakeFestivalGoerRepository implements IFestivalGoerRepository {
        private final List<FestivalGoerBalanceEntry> store = new ArrayList<>();

        void add(String festivalGoerId, FestivalGoerBalance balance) {
            store.removeIf(e -> e.festivalGoerId.equals(festivalGoerId));
            store.add(new FestivalGoerBalanceEntry(festivalGoerId, balance));
        }

        FestivalGoerBalance findBalance(String festivalGoerId) {
            return store.stream()
                    .filter(e -> e.festivalGoerId.equals(festivalGoerId))
                    .map(e -> e.balance)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Festival goer not found: " + festivalGoerId));
        }

        @Override
        public FestivalGoerBalance getBalance(String festivalGoerId) {
            return findBalance(festivalGoerId);
        }

        @Override
        public FestivalGoerBalance saveBalance(FestivalGoerBalance balance) {
            // In a real repository, this would persist to database
            // For testing, we just return the same balance
            return balance;
        }

        private static class FestivalGoerBalanceEntry {
            String festivalGoerId;
            FestivalGoerBalance balance;

            FestivalGoerBalanceEntry(String festivalGoerId, FestivalGoerBalance balance) {
                this.festivalGoerId = festivalGoerId;
                this.balance = balance;
            }
        }
    }

    /**
     * Fake implementation of the order repository for testing.
     * Stores orders by ID in memory.
     */
    static class FakeOrderRepository implements IOrderRepository {
        private final List<Order> store = new ArrayList<>();

        void add(Order order) {
            store.removeIf(o -> o.getOrderId().equals(order.getOrderId()));
            store.add(order);
        }

        Optional<Order> find(String orderId) {
            return store.stream()
                    .filter(o -> o.getOrderId().equals(orderId))
                    .findFirst();
        }

        @Override
        public Order save(Order order) {
            add(order);
            return order;
        }

        @Override
        public Optional<Order> findById(String orderId) {
            return find(orderId);
        }

        List<Order> findAll() {
            return List.copyOf(store);
        }
    }
}
