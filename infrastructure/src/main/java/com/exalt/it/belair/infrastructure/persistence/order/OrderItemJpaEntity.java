package com.exalt.it.belair.infrastructure.persistence.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity mapping for OrderItem persistence.
 * Represents individual items within an order.
 */
@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "item_type")
    private String itemType;
    
    @Column(name = "item_subtype")
    private String itemSubtype;
    
    @Column
    private int quantity;
    
    /**
     * Default constructor for JPA.
     */
    public OrderItemJpaEntity() {
    }
    
    /**
     * Constructs an OrderItemJpaEntity with the specified parameters.
     * 
     * @param orderId the order ID this item belongs to
     * @param itemType the type of item (DRINK, FOOD)
     * @param itemSubtype the subtype enum name
     * @param quantity the quantity ordered
     */
    public OrderItemJpaEntity(String orderId, String itemType, String itemSubtype, int quantity) {
        this.orderId = orderId;
        this.itemType = itemType;
        this.itemSubtype = itemSubtype;
        this.quantity = quantity;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getItemType() {
        return itemType;
    }
    
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }
    
    public String getItemSubtype() {
        return itemSubtype;
    }
    
    public void setItemSubtype(String itemSubtype) {
        this.itemSubtype = itemSubtype;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
