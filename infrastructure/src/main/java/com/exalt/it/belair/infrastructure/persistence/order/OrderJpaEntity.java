package com.exalt.it.belair.infrastructure.persistence.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.List;

/**
 * JPA Entity mapping for Order persistence.
 * Represents the 'orders' database table.
 */
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    
    @Id
    private String id;
    
    @Column(name = "festival_goer_id")
    private String festivalGoerId;
    
    @Column
    private String status;
    
    /**
     * Items in this order (transient - not persisted to database).
     * In a full JPA implementation with @OneToMany cascade, this would be a persistent collection.
     * For now, it's stored in-memory during adapter operations.
     */
    @Transient
    private List<OrderItemJpaEntity> items;
    
    /**
     * Default constructor for JPA.
     */
    public OrderJpaEntity() {
    }
    
    /**
     * Constructs an OrderJpaEntity with the specified parameters.
     * 
     * @param id the order ID
     * @param festivalGoerId the festival goer ID
     * @param status the order status
     */
    public OrderJpaEntity(String id, String festivalGoerId, String status) {
        this.id = id;
        this.festivalGoerId = festivalGoerId;
        this.status = status;
        this.items = List.of();
    }
    
    /**
     * Constructs an OrderJpaEntity with items.
     * 
     * @param id the order ID
     * @param festivalGoerId the festival goer ID
     * @param status the order status
     * @param items the order items
     */
    public OrderJpaEntity(String id, String festivalGoerId, String status, List<OrderItemJpaEntity> items) {
        this.id = id;
        this.festivalGoerId = festivalGoerId;
        this.status = status;
        this.items = items;
    }
    
    /**
     * Gets the order ID.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the order ID.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the festival goer ID.
     */
    public String getFestivalGoerId() {
        return festivalGoerId;
    }
    
    /**
     * Sets the festival goer ID.
     */
    public void setFestivalGoerId(String festivalGoerId) {
        this.festivalGoerId = festivalGoerId;
    }
    
    /**
     * Gets the order status.
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets the order status.
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Gets the order items.
     */
    public List<OrderItemJpaEntity> getItems() {
        return items;
    }
    
    /**
     * Sets the order items.
     */
    public void setItems(List<OrderItemJpaEntity> items) {
        this.items = items;
    }
}
