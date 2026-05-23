package com.example.userservice.controller;

import com.example.userservice.model.Order;
import com.example.userservice.model.OrderRequest;
import com.example.userservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * REST controller for orders. Accepts HTTP POST requests and delegates to
 * OrderService to persist the order and emit an event. Reactive types are
 * returned to allow for non‑blocking handling.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }
}