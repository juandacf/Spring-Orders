package com.example.deliveryservice.controller;

import com.example.deliveryservice.model.OrderRatingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint for rating orders. Ratings are stored on the order item in DynamoDB.
 */
@RestController
@RequestMapping("/orders")
public class RatingController {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    public RatingController(DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    }

    @PostMapping("/{orderId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> rateOrder(@PathVariable String orderId, @RequestBody OrderRatingRequest request) {
        return Mono.fromFuture(() -> {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("orderId", AttributeValue.builder().s(orderId).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#rating", "rating");

            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":r", AttributeValue.builder().n(Integer.toString(request.getRating())).build());

            UpdateItemRequest update = UpdateItemRequest.builder()
                .tableName("Orders")
                .key(key)
                .updateExpression("SET #rating = :r")
                .expressionAttributeNames(expressionNames)
                .expressionAttributeValues(expressionValues)
                .build();

            return dynamoDbAsyncClient.updateItem(update)
                .thenAccept(resp -> {});
        });
    }
}