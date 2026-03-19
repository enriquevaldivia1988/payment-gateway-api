package com.codebugs.payment.service;

import com.codebugs.payment.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for payment processing operations.
 * Defines the contract for authorization, capture, refund, and reversal flows.
 */
public interface PaymentService {

    /**
     * Creates and authorizes a new payment against the card processor.
     *
     * @param payment the payment request
     * @return the created payment with AUTHORIZED or FAILED status
     */
    Payment createPayment(Payment payment);

    /**
     * Retrieves a payment by its ID, scoped to a specific merchant.
     *
     * @param id         the payment UUID
     * @param merchantId the merchant identifier for tenant isolation
     * @return the payment if found within the merchant's scope
     */
    Optional<Payment> getPayment(UUID id, String merchantId);

    /**
     * Captures a previously authorized payment, settling the funds.
     *
     * @param id         the payment UUID
     * @param merchantId the merchant identifier for tenant isolation
     * @return the captured payment
     * @throws IllegalStateException if the payment is not in AUTHORIZED status
     */
    Payment capturePayment(UUID id, String merchantId);

    /**
     * Refunds a previously captured payment.
     *
     * @param id         the payment UUID
     * @param merchantId the merchant identifier for tenant isolation
     * @return the refunded payment
     * @throws IllegalStateException if the payment is not in CAPTURED status
     */
    Payment refundPayment(UUID id, String merchantId);

    /**
     * Reverses (voids) a previously authorized payment before capture.
     *
     * @param id         the payment UUID
     * @param merchantId the merchant identifier for tenant isolation
     * @return the reversed payment
     * @throws IllegalStateException if the payment is not in AUTHORIZED status
     */
    Payment reversePayment(UUID id, String merchantId);
}
