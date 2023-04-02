package com.jolly.springstatemachine.config;

import com.jolly.springstatemachine.domain.PaymentEvent;
import com.jolly.springstatemachine.domain.PaymentState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StateMachineConfigTest {

    @Autowired
    StateMachineFactory<PaymentState, PaymentEvent> factory;

    @Test
    void testNewStateMachine() {
        StateMachine<PaymentState, PaymentEvent> sm = factory.getStateMachine(UUID.randomUUID());

        sm.start();

        System.out.println(sm.getState().toString());

        sm.sendEvent(PaymentEvent.PRE_AUTHORIZE);

        System.out.println(sm.getState().toString());

        sm.sendEvent(PaymentEvent.PRE_AUTH_APPROVED);

        System.out.println(sm.getState().toString());

        // PaymentState should stay in PRE_AUTH after
        // PRE_AUTH_APPROVED since we did not configure
        // any state change from PRE_AUTH in [StateMachineConfig]
        sm.sendEvent(PaymentEvent.PRE_AUTH_DECLINED);

        System.out.println(sm.getState().toString());
    }
}