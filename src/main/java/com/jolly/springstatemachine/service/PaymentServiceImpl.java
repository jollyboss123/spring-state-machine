package com.jolly.springstatemachine.service;

import com.jolly.springstatemachine.domain.Payment;
import com.jolly.springstatemachine.domain.PaymentEvent;
import com.jolly.springstatemachine.domain.PaymentState;
import com.jolly.springstatemachine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService{
    public static final String PAYMENT_HEADER_ID = "payment_id";

    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;
    private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;

    @Override
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        //TODO: throw custom error state machine not found
        if (sm == null) return null;

        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);

        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        if (sm == null) return null;

        sendEvent(paymentId, sm, PaymentEvent.AUTH_APPROVED);

        return sm;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        if (sm == null) return null;

        sendEvent(paymentId, sm, PaymentEvent.AUTH_DECLINED);

        return sm;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event) {
        Message<PaymentEvent> msg = MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_HEADER_ID, paymentId)
                .build();

        sm.sendEvent(msg);
    }

    // to rehydrate state machine
    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        //TODO: throw custom error payment not found
        if (payment == null) return null;

        StateMachine<PaymentState, PaymentEvent> sm = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

        System.out.println("Resetting state machine");
        sm.stop();

        // reset state to the state set in DB
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(paymentStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(), null, null
                    , null
                    ));
                });

        sm.start();
        System.out.println("Resetted state machine");

        return sm;
    }
}
