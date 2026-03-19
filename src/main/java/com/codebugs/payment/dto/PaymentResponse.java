package com.codebugs.payment.dto;

import com.codebugs.payment.model.Payment;
import com.codebugs.payment.model.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String merchantId,
        Long amount,
        String currency,
        PaymentStatus status,
        String cardLastFour,
        String processorRef,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCardLastFour(),
                payment.getProcessorRef(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
