package com.example.userservice.model;

/**
 * Request DTO used by the REST controller. Contains only the information
 * provided by the client when creating an order. The server generates the
 * order identifier and sets the initial status.
 */
public class OrderRequest {
    private String customerId;
    private OrderType type;

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
}