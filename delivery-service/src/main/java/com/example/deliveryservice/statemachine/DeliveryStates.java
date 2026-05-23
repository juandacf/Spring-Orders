package com.example.deliveryservice.statemachine;

/**
 * Enumerates the possible states for the order state machine in the delivery
 * service. These states mirror the status values persisted on the order
 * entity.
 */
public enum DeliveryStates {
    ORDER_RECEIVED,
    PREPARING,
    ASSIGNING_DELIVERY_STAFF,
    ON_ROUTE,
    DELIVERED,
    CANCELLED
}