package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.core.annotation.DomainUseCase;
import com.exalt.it.belair.domain.order.events.OrderAcknowledgedEvent;
import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeAcknowledgedException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.in.AcknowledgeOrderUseCasePort;
import com.exalt.it.belair.domain.order.ports.out.IEventPublisher;
import com.exalt.it.belair.domain.order.ports.out.IFestivalGoerRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import com.exalt.it.belair.domain.order.services.EstimatedTimeCalculator;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Use case for acknowledging a pending order.
 */
@DomainUseCase
public class AcknowledgeOrderUseCase implements AcknowledgeOrderUseCasePort {
    private final IOrderRepository orderRepository;
    private final IFestivalGoerRepository festivalGoerRepository;
    private final IEventPublisher eventPublisher;
    private final EstimatedTimeCalculator estimatedTimeCalculator;

    public AcknowledgeOrderUseCase(
            IOrderRepository orderRepository,
            IFestivalGoerRepository festivalGoerRepository,
            IEventPublisher eventPublisher
    ) {
        this(orderRepository, festivalGoerRepository, eventPublisher, new EstimatedTimeCalculator());
    }

    public AcknowledgeOrderUseCase(
            IOrderRepository orderRepository,
            IFestivalGoerRepository festivalGoerRepository,
            IEventPublisher eventPublisher,
            EstimatedTimeCalculator estimatedTimeCalculator
    ) {
        this.orderRepository = orderRepository;
        this.festivalGoerRepository = festivalGoerRepository;
        this.eventPublisher = eventPublisher;
        this.estimatedTimeCalculator = estimatedTimeCalculator;
    }

    @Override
    public void execute(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (!OrderStatusEnum.PENDING.equals(order.getStatus())) {
            throw new OrderCannotBeAcknowledgedException(
                    "Order cannot be acknowledged from status: " + order.getStatus()
            );
        }

        FestivalGoerBalance balance = festivalGoerRepository.getBalance(order.getFestivalGoerId());
        balance.consumeTokens(order.getReservedDrinkTokens(), order.getReservedSnackTokens());

        int estimatedMinutes = estimatedTimeCalculator.calculateEstimatedTimeMinutes(order);
        Instant now = Instant.now();
        LocalDateTime estimatedReadinessAt = LocalDateTime.now().plusMinutes(estimatedMinutes);

        order.setStatus(OrderStatusEnum.ACKNOWLEDGED);
        order.setEstimatedReadinessAt(estimatedReadinessAt);
        order.setEstimatedReadinessMinutes(estimatedMinutes);
        order.setUpdatedAt(now);

        festivalGoerRepository.saveBalance(balance);
        orderRepository.save(order);

        eventPublisher.publish(new OrderAcknowledgedEvent(
                order.getId(),
                order.getFestivalGoerId(),
                estimatedReadinessAt,
                now
        ));
    }
}
