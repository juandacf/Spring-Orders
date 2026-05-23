package com.example.deliveryservice.statemachine;

/**
 * Enumerates events that trigger state transitions in the delivery service
 * state machine. Each event corresponds to a step in fulfilling an order.
 */
public enum DeliveryEvents {
    PREPARE,
    ASSIGN,
    DISPATCH,
    COMPLETE,
    CANCEL
}