package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.order.events.OrderAcknowledgedEvent;
import com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException;
import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeAcknowledgedException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IEventPublisher;
import com.exalt.it.belair.domain.order.ports.out.IFestivalGoerRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcknowledgeOrderUseCaseTest {
    private AcknowledgeOrderUseCase sut;
    private TestOrderRepository orderRepository;
    private TestFestivalGoerRepository festivalGoerRepository;
    private TestEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository = new TestOrderRepository();
        festivalGoerRepository = new TestFestivalGoerRepository();
        eventPublisher = new TestEventPublisher();
        sut = new AcknowledgeOrderUseCase(orderRepository, festivalGoerRepository, eventPublisher);
    }

    @Test
    void execute_shouldAcknowledgePendingOrderConsumeTokensAndPublishEvent() {
        String orderId = "ord-001";
        String festivalGoerId = "fgv-001";
        FestivalGoerBalance balance = new FestivalGoerBalance(6, 9);
        balance.reserveDrinkTokens(2);
        balance.reserveSnackTokens(3);
        festivalGoerRepository.addBalance(festivalGoerId, balance);

        Order order = new Order(
                orderId,
                festivalGoerId,
                List.of(OrderItem.createDrinkItem(DrinkTypeEnum.PREMIUM_ALCOHOLIC, 1)),
                OrderStatusEnum.PENDING
        );
        order.setReservedTokens(2, 3);
        orderRepository.add(order);

        sut.execute(orderId);

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatusEnum.ACKNOWLEDGED);
        assertThat(updatedOrder.getEstimatedReadinessMinutes()).isEqualTo(3);
        assertThat(updatedOrder.getEstimatedReadinessAt()).isNotNull();
        assertThat(updatedOrder.getUpdatedAt()).isNotNull();

        FestivalGoerBalance updatedBalance = festivalGoerRepository.getBalance(festivalGoerId);
        assertThat(updatedBalance.getDrinkTokens()).isEqualTo(4);
        assertThat(updatedBalance.getReservedDrinkTokens()).isEqualTo(0);
        assertThat(updatedBalance.getSnackTokens()).isEqualTo(6);
        assertThat(updatedBalance.getReservedSnackTokens()).isEqualTo(0);

        assertThat(eventPublisher.events).hasSize(1);
        OrderAcknowledgedEvent event = eventPublisher.events.get(0);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getFestivalGoerId()).isEqualTo(festivalGoerId);
        assertThat(event.getEstimatedReadinessAt()).isEqualTo(updatedOrder.getEstimatedReadinessAt());
        assertThat(Duration.between(LocalDateTime.now(), event.getEstimatedReadinessAt()).toMinutes())
                .isBetween(1L, 3L);
        assertThat(event.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void execute_shouldThrowWhenOrderIsNotFound() {
        assertThatThrownBy(() -> sut.execute("ord-missing"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void execute_shouldThrowWhenOrderIsNotPending() {
        String orderId = "ord-ack";
        Order order = new Order(orderId, "fgv-001", List.of(), OrderStatusEnum.ACKNOWLEDGED);
        orderRepository.add(order);

        assertThatThrownBy(() -> sut.execute(orderId))
                .isInstanceOf(OrderCannotBeAcknowledgedException.class)
                .hasMessageContaining("ACKNOWLEDGED");
    }

    @Test
    void execute_shouldThrowWhenFestivalGoerIsNotFound() {
        String orderId = "ord-001";
        Order order = new Order(orderId, "fgv-missing", List.of(), OrderStatusEnum.PENDING);
        orderRepository.add(order);

        assertThatThrownBy(() -> sut.execute(orderId))
                .isInstanceOf(FestivalGoerNotFoundException.class);
    }

    static class TestOrderRepository implements IOrderRepository {
        private final List<Order> store = new ArrayList<>();

        void add(Order order) {
            store.removeIf(current -> current.getId().equals(order.getId()));
            store.add(order);
        }

        @Override
        public Order save(Order order) {
            add(order);
            return order;
        }

        @Override
        public Optional<Order> findById(String orderId) {
            return store.stream().filter(current -> current.getId().equals(orderId)).findFirst();
        }
    }

    static class TestFestivalGoerRepository implements IFestivalGoerRepository {
        private final List<FestivalGoerBalanceEntry> balances = new ArrayList<>();

        void addBalance(String festivalGoerId, FestivalGoerBalance balance) {
            balances.removeIf(entry -> entry.festivalGoerId.equals(festivalGoerId));
            balances.add(new FestivalGoerBalanceEntry(festivalGoerId, balance));
        }

        @Override
        public FestivalGoerBalance getBalance(String festivalGoerId) {
            return balances.stream()
                    .filter(entry -> entry.festivalGoerId.equals(festivalGoerId))
                    .map(entry -> entry.balance)
                    .findFirst()
                    .orElseThrow(() -> new FestivalGoerNotFoundException("Festival goer not found: " + festivalGoerId));
        }

        @Override
        public FestivalGoerBalance saveBalance(FestivalGoerBalance balance) {
            return balance;
        }

        private record FestivalGoerBalanceEntry(String festivalGoerId, FestivalGoerBalance balance) {
        }
    }

    static class TestEventPublisher implements IEventPublisher {
        private final List<OrderAcknowledgedEvent> events = new ArrayList<>();

        @Override
        public void publish(Object event) {
            if (event instanceof OrderAcknowledgedEvent acknowledgedEvent) {
                events.add(acknowledgedEvent);
            }
        }
    }
}
