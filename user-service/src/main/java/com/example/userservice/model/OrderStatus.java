package com.example.userservice.model;

/**
 * Enumeration representing the life‑cycle states of an order. These states
 * correspond to the state machine used by the delivery service. When a new
 * order is created it starts in the {@link #ORDER_RECEIVED} state and
 * transitions over time through the following states:
 *
 * <ul>
 *   <li>{@link #PREPARING} – the order is being prepared.</li>
 *   <li>{@link #ASSIGNING_DELIVERY_STAFF} – a courier is being assigned.</li>
 *   <li>{@link #ON_ROUTE} – the courier is en route.</li>
 *   <li>{@link #DELIVERED} – the order has been delivered.</li>
 *   <li>{@link #CANCELLED} – the order has been cancelled.</li>
 * </ul>
 */
public enum OrderStatus {
    ORDER_RECEIVED,
    PREPARING,
    ASSIGNING_DELIVERY_STAFF,
    ON_ROUTE,
    DELIVERED,
    CANCELLED
}