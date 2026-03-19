package com.codebugs.payment.service;

import com.codebugs.payment.exception.PaymentNotFoundException;
import com.codebugs.payment.exception.PaymentStateException;
import com.codebugs.payment.model.Payment;
import com.codebugs.payment.model.PaymentStatus;
import com.codebugs.payment.processor.CardProcessor;
import com.codebugs.payment.processor.ProcessorResult;
import com.codebugs.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final CardProcessor cardProcessor;

    public PaymentServiceImpl(PaymentRepository paymentRepository, CardProcessor cardProcessor) {
        this.paymentRepository = paymentRepository;
        this.cardProcessor = cardProcessor;
    }

    @Override
    public Payment createPayment(Payment payment) {
        log.info("Processing payment authorization for merchant={} amount={} currency={}",
                payment.getMerchantId(), payment.getAmount(), payment.getCurrency());

        if (payment.getIdempotencyKey() != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(payment.getIdempotencyKey());
            if (existing.isPresent()) {
                log.warn("Duplicate idempotency key detected: {}", payment.getIdempotencyKey());
                return existing.get();
            }
        }

        ProcessorResult result = cardProcessor.authorize(payment);

        if (result.success()) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setProcessorRef(result.referenceId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Authorization declined: {}", result.declineReason());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created id={} status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "payments", key = "#id")
    public Optional<Payment> getPayment(UUID id, String merchantId) {
        log.debug("Fetching payment id={} for merchant={}", id, merchantId);
        return paymentRepository.findByIdAndMerchantId(id, merchantId);
    }

    @Override
    @CacheEvict(value = "payments", key = "#id")
    public Payment capturePayment(UUID id, String merchantId) {
        Payment payment = findPaymentOrThrow(id, merchantId);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentStateException(payment.getStatus(), PaymentStatus.AUTHORIZED);
        }

        ProcessorResult result = cardProcessor.capture(payment);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setProcessorRef(result.referenceId());

        log.info("Payment captured id={}", id);
        return paymentRepository.save(payment);
    }

    @Override
    @CacheEvict(value = "payments", key = "#id")
    public Payment refundPayment(UUID id, String merchantId) {
        Payment payment = findPaymentOrThrow(id, merchantId);

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new PaymentStateException(payment.getStatus(), PaymentStatus.CAPTURED);
        }

        ProcessorResult result = cardProcessor.refund(payment);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setProcessorRef(result.referenceId());

        log.info("Payment refunded id={}", id);
        return paymentRepository.save(payment);
    }

    @Override
    @CacheEvict(value = "payments", key = "#id")
    public Payment reversePayment(UUID id, String merchantId) {
        Payment payment = findPaymentOrThrow(id, merchantId);

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentStateException(payment.getStatus(), PaymentStatus.AUTHORIZED);
        }

        ProcessorResult result = cardProcessor.reverse(payment);
        payment.setStatus(PaymentStatus.REVERSED);
        payment.setProcessorRef(result.referenceId());

        log.info("Payment reversed id={}", id);
        return paymentRepository.save(payment);
    }

    private Payment findPaymentOrThrow(UUID id, String merchantId) {
        return paymentRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
