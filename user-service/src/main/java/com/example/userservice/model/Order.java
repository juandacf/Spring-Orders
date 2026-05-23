package com.example.userservice.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain representation of a user order. This class models the data persisted
 * in DynamoDB and sent as events to Kafka. Note that DynamoDB does not
 * require a rigid schema; however, using a well defined model on the
 * application side helps maintain consistency.
 */
public class Order {
    private String orderId;
    private String customerId;
    private OrderType type;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer rating;

    public Order() {
        // default constructor for Jackson and DynamoDB mappers
    }

    public Order(String orderId, String customerId, OrderType type, OrderStatus status,
                 Instant createdAt, Instant updatedAt, Integer rating) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.rating = rating;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}