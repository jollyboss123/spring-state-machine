package com.jolly.springstatemachine.service;

import com.jolly.springstatemachine.domain.Payment;
import com.jolly.springstatemachine.domain.PaymentEvent;
import com.jolly.springstatemachine.domain.PaymentState;
import com.jolly.springstatemachine.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .amount(new BigDecimal("12.99"))
                .build();
    }

    @Test
    void preAuth() {
        Payment savedPayment = paymentService.newPayment(payment);

        assertSame(savedPayment.getState(), PaymentState.NEW);

        StateMachine<PaymentState, PaymentEvent> sm = paymentService.preAuth(savedPayment.getId());

        paymentRepository.findById(savedPayment.getId())
                .ifPresent(System.out::println);

        List<PaymentState> preAuthStates = Arrays.asList(PaymentState.PRE_AUTH, PaymentState.PRE_AUTH_ERROR);

        assertTrue(preAuthStates.contains(sm.getState().getId()));
    }
}