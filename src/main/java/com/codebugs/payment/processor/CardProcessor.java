package com.codebugs.payment.processor;

import com.codebugs.payment.model.Payment;

/**
 * Abstraction for external card processor integration.
 * Implementations can target Stripe, Adyen, or any PSP.
 */
public interface CardProcessor {

    ProcessorResult authorize(Payment payment);

    ProcessorResult capture(Payment payment);

    ProcessorResult refund(Payment payment);

    ProcessorResult reverse(Payment payment);
}
