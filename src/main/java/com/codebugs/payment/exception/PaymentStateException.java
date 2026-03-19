package com.codebugs.payment.exception;

import com.codebugs.payment.model.PaymentStatus;

public class PaymentStateException extends RuntimeException {

    public PaymentStateException(PaymentStatus current, PaymentStatus required) {
        super("Payment must be in " + required + " status. Current: " + current);
    }
}
