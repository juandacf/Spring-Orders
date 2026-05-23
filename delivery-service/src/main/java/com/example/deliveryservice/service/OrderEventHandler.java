package com.example.deliveryservice.service;

import com.example.deliveryservice.model.OrderEventPayload;
import com.example.deliveryservice.statemachine.DeliveryEvents;
import com.example.deliveryservice.statemachine.DeliveryStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles incoming order events from Kafka. For each order type a specific
 * function sequence is executed on a newly created state machine instance.
 * The state machine transitions correspond to the business process for the
 * order type. In a production system you would also persist intermediate
 * states and publish domain events when transitions occur.
 */
@Service
public class OrderEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    private final StateMachineFactory<DeliveryStates, DeliveryEvents> stateMachineFactory;
    private final Map<String, Function<StateMachine<DeliveryStates, DeliveryEvents>, Mono<Void>>> processors = new HashMap<>();

    @Autowired
    public OrderEventHandler(StateMachineFactory<DeliveryStates, DeliveryEvents> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

    @PostConstruct
    public void setupProcessors() {
        // Normal order: prepare -> assign -> dispatch -> complete
        processors.put("NORMAL", sm -> {
            return sm.startReactively()
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.PREPARE).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then());
        });
        // Priority: maybe skip prepare and directly assign and dispatch
        processors.put("PRIORITY", sm -> {
            return sm.startReactively()
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then());
        });
        // Pickup: only prepare then complete (customer picks up)
        processors.put("PICKUP", sm -> {
            return sm.startReactively()
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.PREPARE).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then());
        });
        // No preparation: assign immediately then complete
        processors.put("NO_PREPARATION", sm -> {
            return sm.startReactively()
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
                .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then());
        });
    }

    @KafkaListener(topics = "order-events", groupId = "delivery-group")
    public void listen(@Payload OrderEventPayload payload) {
        log.info("Received order event: {}", payload);
        StateMachine<DeliveryStates, DeliveryEvents> stateMachine = stateMachineFactory.getStateMachine(payload.getOrderId());

        if ("CANCELLED".equals(payload.getStatus())) {
            log.info("Processing cancellation for order: {}", payload.getOrderId());
            stateMachine.startReactively()
                .then(stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.CANCEL).build())).then())
                .subscribe();
            return;
        }

        String type = payload.getType();
        Function<StateMachine<DeliveryStates, DeliveryEvents>, Mono<Void>> processor = processors.get(type);
        if (processor == null) {
            log.error("Received order with unknown type: {}", type);
            return;
        }
        processor.apply(stateMachine).subscribe();
        // In a real application, save updated state to database and possibly emit events
    }
}