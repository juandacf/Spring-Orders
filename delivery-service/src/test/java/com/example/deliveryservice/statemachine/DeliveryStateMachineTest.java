package com.example.deliveryservice.statemachine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DeliveryStateMachineTest {

    @Autowired
    private StateMachineFactory<DeliveryStates, DeliveryEvents> stateMachineFactory;

    @Test
    public void testNormalOrderWorkflow() {
        StateMachine<DeliveryStates, DeliveryEvents> sm = stateMachineFactory.getStateMachine("normal-order");
        sm.startReactively()
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.PREPARE).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then())
            .block();

        assertEquals(DeliveryStates.DELIVERED, sm.getState().getId());
    }

    @Test
    public void testPriorityOrderWorkflow() {
        StateMachine<DeliveryStates, DeliveryEvents> sm = stateMachineFactory.getStateMachine("priority-order");
        sm.startReactively()
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then())
            .block();

        assertEquals(DeliveryStates.DELIVERED, sm.getState().getId(), "Priority order should be DELIVERED");
    }

    @Test
    public void testPickupOrderWorkflow() {
        StateMachine<DeliveryStates, DeliveryEvents> sm = stateMachineFactory.getStateMachine("pickup-order");
        sm.startReactively()
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.PREPARE).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then())
            .block();

        assertEquals(DeliveryStates.DELIVERED, sm.getState().getId(), "Pickup order should be DELIVERED");
    }

    @Test
    public void testNoPreparationOrderWorkflow() {
        StateMachine<DeliveryStates, DeliveryEvents> sm = stateMachineFactory.getStateMachine("no-prep-order");
        sm.startReactively()
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.ASSIGN).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.DISPATCH).build())).then())
            .then(sm.sendEvent(Mono.just(MessageBuilder.withPayload(DeliveryEvents.COMPLETE).build())).then())
            .block();

        assertEquals(DeliveryStates.DELIVERED, sm.getState().getId(), "No preparation order should be DELIVERED");
    }
}
