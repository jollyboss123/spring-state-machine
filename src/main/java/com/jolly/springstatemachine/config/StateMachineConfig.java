package com.jolly.springstatemachine.config;

import com.jolly.springstatemachine.domain.PaymentEvent;
import com.jolly.springstatemachine.domain.PaymentState;
import com.jolly.springstatemachine.service.PaymentService;
import com.jolly.springstatemachine.service.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

@Slf4j
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH) // Happy path
                .end(PaymentState.PRE_AUTH_ERROR) // terminal error state
                .end(PaymentState.AUTH_ERROR); // terminal error state
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZE).action(preAuthAction())
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED)
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(PaymentEvent.AUTHORIZE).action(authAction())
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTH_APPROVED)
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTH_DECLINED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<PaymentState, PaymentEvent>() {
            @Override
            public void stateChanged(State from, State to) {
                log.info(String.format("state changed from: %s to: %s",
                        from != null ? from.getId() : "null",
                        to != null ? to.getId() : "null"
                ));
            }
        };

        config.withConfiguration().listener(adapter);
    }

    public Action<PaymentState, PaymentEvent> preAuthAction() {
        return stateContext -> {
            System.out.println("------ Pre Auth Action ------");

            if (new Random().nextInt(10) < 8) {
                System.out.println("Pre Auth Approved");
                stateContext.getStateMachine()
                        .sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED)
                                .setHeader(PaymentServiceImpl.PAYMENT_HEADER_ID, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_HEADER_ID))
                                .build());
            } else {
                System.out.println("Pre Auth Declined");
                stateContext.getStateMachine()
                        .sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)
                                .setHeader(PaymentServiceImpl.PAYMENT_HEADER_ID, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_HEADER_ID))
                                .build());
            }
        };
    }

    public Action<PaymentState, PaymentEvent> authAction() {
        return stateContext -> {
            System.out.println("------ Auth Action ------");

            if (new Random().nextInt(10) < 8) {
                System.out.println("Auth Approved");
                stateContext.getStateMachine()
                        .sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_APPROVED)
                                .setHeader(PaymentServiceImpl.PAYMENT_HEADER_ID, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_HEADER_ID))
                                .build());
            } else {
                System.out.println("Auth Declined");
                stateContext.getStateMachine()
                        .sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTH_DECLINED)
                                .setHeader(PaymentServiceImpl.PAYMENT_HEADER_ID, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_HEADER_ID))
                                .build());
            }
        };
    }
}
