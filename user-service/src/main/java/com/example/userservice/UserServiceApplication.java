package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the user service. This service exposes a reactive HTTP API
 * for creating orders. When an order is created it is saved to DynamoDB and
 * the event is published to Kafka for downstream processing.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}