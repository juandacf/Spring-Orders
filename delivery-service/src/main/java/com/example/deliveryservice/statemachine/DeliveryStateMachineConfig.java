package com.example.deliveryservice.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.listener.StateMachineListener;

import java.util.EnumSet;

/**
 * Configuration for the delivery state machine. The machine defines the
 * allowed transitions between order states and emits events when state
 * changes occur. A factory bean is exposed so that multiple state
 * machine instances can be created per order.
 */
@Configuration
@EnableStateMachineFactory
public class DeliveryStateMachineConfig extends StateMachineConfigurerAdapter<DeliveryStates, DeliveryEvents> {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStateMachineConfig.class);

    @Override
    public void configure(StateMachineStateConfigurer<DeliveryStates, DeliveryEvents> states) throws Exception {
        states.withStates()
            .initial(DeliveryStates.ORDER_RECEIVED)
            .states(EnumSet.allOf(DeliveryStates.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<DeliveryStates, DeliveryEvents> transitions) throws Exception {
        transitions
            .withExternal()
                .source(DeliveryStates.ORDER_RECEIVED).target(DeliveryStates.PREPARING).event(DeliveryEvents.PREPARE)
                .and()
            .withExternal()
                .source(DeliveryStates.ORDER_RECEIVED).target(DeliveryStates.ASSIGNING_DELIVERY_STAFF).event(DeliveryEvents.ASSIGN)
                .and()
            .withExternal()
                .source(DeliveryStates.PREPARING).target(DeliveryStates.ASSIGNING_DELIVERY_STAFF).event(DeliveryEvents.ASSIGN)
                .and()
            .withExternal()
                .source(DeliveryStates.PREPARING).target(DeliveryStates.DELIVERED).event(DeliveryEvents.COMPLETE)
                .and()
            .withExternal()
                .source(DeliveryStates.ASSIGNING_DELIVERY_STAFF).target(DeliveryStates.ON_ROUTE).event(DeliveryEvents.DISPATCH)
                .and()
            .withExternal()
                .source(DeliveryStates.ON_ROUTE).target(DeliveryStates.DELIVERED).event(DeliveryEvents.COMPLETE)
                .and()
            .withExternal()
                .source(DeliveryStates.ORDER_RECEIVED).target(DeliveryStates.CANCELLED).event(DeliveryEvents.CANCEL)
                .and()
            .withExternal()
                .source(DeliveryStates.PREPARING).target(DeliveryStates.CANCELLED).event(DeliveryEvents.CANCEL)
                .and()
            .withExternal()
                .source(DeliveryStates.ASSIGNING_DELIVERY_STAFF).target(DeliveryStates.CANCELLED).event(DeliveryEvents.CANCEL)
                .and()
            .withExternal()
                .source(DeliveryStates.ON_ROUTE).target(DeliveryStates.CANCELLED).event(DeliveryEvents.CANCEL);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<DeliveryStates, DeliveryEvents> config) throws Exception {
        config.withConfiguration()
            .listener(loggingListener());
    }

    /**
     * Simple listener that logs state changes. In a production system you
     * might publish domain events here or persist the new state to the
     * database.
     */
    @Bean
    public StateMachineListener<DeliveryStates, DeliveryEvents> loggingListener() {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<DeliveryStates, DeliveryEvents> from,
                                     State<DeliveryStates, DeliveryEvents> to) {
                if (from != null) {
                    log.info("Transitioned from {} to {}", from.getId(), to.getId());
                } else {
                    log.info("Entered initial state {}", to.getId());
                }
            }
        };
    }
}