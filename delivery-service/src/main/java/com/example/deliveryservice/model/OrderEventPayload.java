package com.example.deliveryservice.model;

/**
 * Represents the payload sent from the user service when an order is created.
 * The delivery service uses this to construct internal order models and to
 * initialise the state machine.
 */
public class OrderEventPayload {
    private String orderId;
    private String customerId;
    private String type;
    private String status;
    private String timestamp;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}