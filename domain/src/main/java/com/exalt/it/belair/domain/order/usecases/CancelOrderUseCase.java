package com.exalt.it.belair.domain.order.usecases;

import com.exalt.it.belair.domain.core.annotation.DomainUseCase;
import com.exalt.it.belair.domain.order.exceptions.OrderCannotBeCancelledException;
import com.exalt.it.belair.domain.order.exceptions.OrderNotFoundException;
import com.exalt.it.belair.domain.order.model.FestivalGoerBalance;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import com.exalt.it.belair.domain.order.ports.out.IFestivalGoerRepository;
import com.exalt.it.belair.domain.order.ports.out.IOrderRepository;
import java.time.Instant;

/**
 * Use case for cancelling a pending order in the Belair Buvette system.
 * 
 * Orchestrates the order cancellation workflow:
 * 1. Validates order exists
 * 2. Validates order is in PENDING status
 * 3. Loads festival goer's token balance
 * 4. Unreserves tokens associated with the order
 * 5. Updates order status to CANCELLED
 * 6. Persists changes to both order and token balance
 * 
 * Respects the Hexagonal Architecture by delegating persistence to outbound ports.
 */
@DomainUseCase
public class CancelOrderUseCase {
    private final IOrderRepository orderRepository;
    private final IFestivalGoerRepository festivalGoerRepository;

    /**
     * Constructs a CancelOrderUseCase with the required outbound port dependencies.
     * 
     * @param orderRepository the port for persisting orders
     * @param festivalGoerRepository the port for managing festival goer token balances
     */
    public CancelOrderUseCase(
            IOrderRepository orderRepository,
            IFestivalGoerRepository festivalGoerRepository
    ) {
        this.orderRepository = orderRepository;
        this.festivalGoerRepository = festivalGoerRepository;
    }

    /**
     * Executes the order cancellation workflow.
     * 
     * @param orderId the ID of the order to cancel
     * 
     * @throws OrderNotFoundException if order not found
     * @throws OrderCannotBeCancelledException if order is not in PENDING status
     * @throws com.exalt.it.belair.domain.order.exceptions.FestivalGoerNotFoundException if festival goer not found
     */
    public void execute(String orderId) {
        // 1. Load order by ID
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // 2. Validate order is PENDING
        if (!order.getStatus().equals(OrderStatusEnum.PENDING)) {
            throw new OrderCannotBeCancelledException("Order is not in PENDING status");
        }

        // 3. Load festival goer's token balance
        FestivalGoerBalance balance = festivalGoerRepository.getBalance(order.getFestivalGoerId());

        // 4. Unreserve tokens
        balance.unreserveTokens(order.getReservedDrinkTokens(), order.getReservedSnackTokens());

        // 5. Update order status to CANCELLED
        order.setStatus(OrderStatusEnum.CANCELLED);

        // 6. Set updated timestamp
        order.setUpdatedAt(Instant.now());

        // 7. Persist changes
        orderRepository.save(order);
        festivalGoerRepository.saveBalance(balance);
    }
}
