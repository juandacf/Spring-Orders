package com.example.userservice.model;

/**
 * Enumeration of high‑level order types. A type determines how the delivery
 * service will process the order downstream. The available values align with
 * the user specification: priority orders, normal orders, pickup orders and
 * orders that don't require preparation.
 */
public enum OrderType {
    PRIORITY,
    NORMAL,
    PICKUP,
    NO_PREPARATION
}