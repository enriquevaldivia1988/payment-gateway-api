package com.codebugs.payment.processor;

import com.codebugs.payment.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated card processor for development and testing.
 * Follows standard test card conventions:
 * - Cards ending in "0002" are declined (insufficient funds)
 * - Cards ending in "0003" are declined (processor error)
 * - All others are approved
 */
@Component
public class SimulatedCardProcessor implements CardProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedCardProcessor.class);

    @Override
    public ProcessorResult authorize(Payment payment) {
        log.info("Simulated authorize: amount={} currency={} card=****{}",
                payment.getAmount(), payment.getCurrency(), payment.getCardLastFour());

        if ("0002".equals(payment.getCardLastFour())) {
            return ProcessorResult.declined("insufficient_funds");
        }
        if ("0003".equals(payment.getCardLastFour())) {
            return ProcessorResult.declined("processor_error");
        }

        return ProcessorResult.approved("sim_auth_" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public ProcessorResult capture(Payment payment) {
        log.info("Simulated capture: paymentId={}", payment.getId());
        return ProcessorResult.approved("sim_cap_" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public ProcessorResult refund(Payment payment) {
        log.info("Simulated refund: paymentId={}", payment.getId());
        return ProcessorResult.approved("sim_ref_" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public ProcessorResult reverse(Payment payment) {
        log.info("Simulated reverse: paymentId={}", payment.getId());
        return ProcessorResult.approved("sim_rev_" + UUID.randomUUID().toString().substring(0, 8));
    }
}
