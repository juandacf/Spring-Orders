package com.example.userservice.service;

import com.example.userservice.model.Order;
import com.example.userservice.model.OrderRequest;
import com.example.userservice.model.OrderStatus;
import com.example.userservice.model.OutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for creating orders, persisting them to DynamoDB and
 * publishing events via the outbox pattern. This implementation uses a
 * DynamoDB transaction to write both the order record and the outbox
 * record atomically.
 */
@Service
public class OrderService {
    private static final String ORDER_TABLE = "Orders";
    private static final String OUTBOX_TABLE = "Outbox";
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderService(DynamoDbAsyncClient dynamoDbAsyncClient,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a new order. A unique ID is generated for the order. Both
     * the order and the outbox event are saved in a single DynamoDB
     * transaction. Once persisted, the event is asynchronously published to
     * Kafka. The returned Mono completes when the transaction has completed,
     * not when the event is published.
     */
    public Mono<Order> createOrder(OrderRequest request) {
        return Mono.fromCallable(() -> {
            String orderId = UUID.randomUUID().toString();
            Instant now = Instant.now();

            Order order = new Order(orderId, request.getCustomerId(), request.getType(),
                OrderStatus.ORDER_RECEIVED, now, now, null);

            // Build outbox event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", order.getOrderId());
            eventPayload.put("customerId", order.getCustomerId());
            eventPayload.put("type", order.getType().name());
            eventPayload.put("status", order.getStatus().name());
            eventPayload.put("timestamp", now.toString());
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(eventPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event payload", e);
            }

            OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "Order",
                order.getOrderId(),
                "OrderCreated",
                payloadJson,
                now,
                null,
                false
            );

            // Build DynamoDB transaction
            TransactWriteItemsRequest txnRequest = TransactWriteItemsRequest.builder()
                .transactItems(
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(ORDER_TABLE)
                            .item(toAttributeMap(order))
                            .build())
                        .build(),
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(OUTBOX_TABLE)
                            .item(toAttributeMap(event))
                            .build())
                        .build()
                )
                .build();

            CompletableFuture<Void> writeFuture = dynamoDbAsyncClient.transactWriteItems(txnRequest)
                .thenAccept(response -> {
                    // After the transaction completes, publish event asynchronously
                    kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getOrderId(), eventPayload);
                });

            // Wait for the transaction to complete
            try {
                writeFuture.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist order and outbox event", e);
            }

            return order;
        });
    }

    /**
     * Cancel an existing order. Fetches the order, verifies it can be cancelled,
     * updates its status, and saves both the order and outbox event in a transaction.
     */
    public Mono<Order> cancelOrder(String orderId) {
        return Mono.fromCallable(() -> {
            software.amazon.awssdk.services.dynamodb.model.GetItemRequest getRequest = software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
                .tableName(ORDER_TABLE)
                .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))
                .build();
                
            software.amazon.awssdk.services.dynamodb.model.GetItemResponse response = dynamoDbAsyncClient.getItem(getRequest).get();
            if (!response.hasItem() || response.item().isEmpty()) {
                throw new RuntimeException("Order not found");
            }
            
            Map<String, AttributeValue> item = response.item();
            String currentStatus = item.get("status").s();
            if (OrderStatus.CANCELLED.name().equals(currentStatus) || OrderStatus.DELIVERED.name().equals(currentStatus)) {
                throw new RuntimeException("Order cannot be cancelled in status: " + currentStatus);
            }
            
            Order order = new Order(
                orderId,
                item.get("customerId").s(),
                com.example.userservice.model.OrderType.valueOf(item.get("type").s()),
                OrderStatus.CANCELLED,
                Instant.parse(item.get("createdAt").s()),
                Instant.now(),
                item.containsKey("rating") ? Integer.parseInt(item.get("rating").n()) : null
            );

            // Build outbox event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", order.getOrderId());
            eventPayload.put("customerId", order.getCustomerId());
            eventPayload.put("type", order.getType().name());
            eventPayload.put("status", order.getStatus().name());
            eventPayload.put("timestamp", order.getUpdatedAt().toString());
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(eventPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event payload", e);
            }

            OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "Order",
                order.getOrderId(),
                "OrderCancelled",
                payloadJson,
                order.getUpdatedAt(),
                null,
                false
            );

            // Build DynamoDB transaction
            TransactWriteItemsRequest txnRequest = TransactWriteItemsRequest.builder()
                .transactItems(
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(ORDER_TABLE)
                            .item(toAttributeMap(order))
                            .build())
                        .build(),
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(OUTBOX_TABLE)
                            .item(toAttributeMap(event))
                            .build())
                        .build()
                )
                .build();

            CompletableFuture<Void> writeFuture = dynamoDbAsyncClient.transactWriteItems(txnRequest)
                .thenAccept(res -> {
                    // After the transaction completes, publish event asynchronously
                    kafkaTemplate.send(ORDER_EVENTS_TOPIC, order.getOrderId(), eventPayload);
                });

            // Wait for the transaction to complete
            try {
                writeFuture.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist cancelled order and outbox event", e);
            }

            return order;
        });
    }

    /**
     * Convert an Order to a DynamoDB item represented as a map of
     * attribute values. In a production setting you may want to use an
     * object mapper like DynamoDbEnhancedClient, but here we build the map
     * manually for clarity.
     */
    private Map<String, AttributeValue> toAttributeMap(Order order) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.builder().s(order.getOrderId()).build());
        item.put("customerId", AttributeValue.builder().s(order.getCustomerId()).build());
        item.put("type", AttributeValue.builder().s(order.getType().name()).build());
        item.put("status", AttributeValue.builder().s(order.getStatus().name()).build());
        item.put("createdAt", AttributeValue.builder().s(order.getCreatedAt().toString()).build());
        item.put("updatedAt", AttributeValue.builder().s(order.getUpdatedAt().toString()).build());
        if (order.getRating() != null) {
            item.put("rating", AttributeValue.builder().n(Integer.toString(order.getRating())).build());
        }
        return item;
    }

    /**
     * Convert an OutboxEvent to a DynamoDB item.
     */
    private Map<String, AttributeValue> toAttributeMap(OutboxEvent event) {
        Map<String, AttributeValue> item = new HashMap<>();
        // Primary key composed of aggregate id and event id
        item.put("pk", AttributeValue.builder().s(event.getAggregateType() + "#" + event.getAggregateId()).build());
        item.put("sk", AttributeValue.builder().s("EVENT#" + event.getId()).build());
        item.put("eventType", AttributeValue.builder().s(event.getType()).build());
        item.put("payload", AttributeValue.builder().s(event.getPayload()).build());
        item.put("createdAt", AttributeValue.builder().s(event.getCreatedAt().toString()).build());
        item.put("published", AttributeValue.builder().bool(event.isPublished()).build());
        return item;
    }
}