package com.exalt.it.belair.infrastructure.persistence.order;

import com.exalt.it.belair.domain.order.model.DrinkTypeEnum;
import com.exalt.it.belair.domain.order.model.FoodTypeEnum;
import com.exalt.it.belair.domain.order.model.Order;
import com.exalt.it.belair.domain.order.model.OrderItem;
import com.exalt.it.belair.domain.order.model.OrderStatusEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for Order domain model ↔ OrderJpaEntity persistence entity.
 * Handles complete conversion including nested items.
 * Maintains separation between domain (Order) and persistence (OrderJpaEntity) layers.
 */
@Component
public class OrderMapper {
    
    /**
     * Converts a domain Order to a JPA OrderJpaEntity for persistence.
     * Includes complete item information in the entity.
     * 
     * @param order the domain Order with items
     * @return the JPA entity with items
     */
    public OrderJpaEntity toEntity(Order order) {
        List<OrderItemJpaEntity> itemEntities = order.getItems().stream()
            .map(this::itemToEntity)
            .toList();
        
        return new OrderJpaEntity(
            order.getOrderId(),
            order.getFestivalGoerId(),
            order.getStatus().name(),
            itemEntities
        );
    }
    
    /**
     * Converts a JPA OrderJpaEntity back to a domain Order.
     * Reconstructs the complete Order including all items.
     * 
     * @param entity the JPA entity with items
     * @return the complete domain Order with items
     */
    public Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
            .map(this::itemToDomain)
            .toList();
        
        OrderStatusEnum status = OrderStatusEnum.valueOf(entity.getStatus());
        return new Order(
            entity.getId(),
            entity.getFestivalGoerId(),
            items,
            status
        );
    }
    
    /**
     * Converts a domain OrderItem to a persistence OrderItemJpaEntity.
     */
    private OrderItemJpaEntity itemToEntity(OrderItem item) {
        return new OrderItemJpaEntity(
            null,  // orderId not needed for in-memory representation
            item.getItemType(),
            item.getItemSubtype(),
            item.getQuantity()
        );
    }
    
    /**
     * Converts a persistence OrderItemJpaEntity back to a domain OrderItem.
     */
    private OrderItem itemToDomain(OrderItemJpaEntity entity) {
        if ("DRINK".equals(entity.getItemType())) {
            return OrderItem.createDrinkItem(
                DrinkTypeEnum.valueOf(entity.getItemSubtype()),
                entity.getQuantity()
            );
        } else if ("FOOD".equals(entity.getItemType())) {
            return OrderItem.createFoodItem(
                FoodTypeEnum.valueOf(entity.getItemSubtype()),
                entity.getQuantity()
            );
        }
        throw new IllegalArgumentException("Unknown item type: " + entity.getItemType());
    }
}
