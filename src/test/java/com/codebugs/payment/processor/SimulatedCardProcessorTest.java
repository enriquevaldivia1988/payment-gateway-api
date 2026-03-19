package com.codebugs.payment.processor;

import com.codebugs.payment.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedCardProcessorTest {

    private SimulatedCardProcessor processor;
    private Payment payment;

    @BeforeEach
    void setUp() {
        processor = new SimulatedCardProcessor();
        payment = new Payment();
        payment.setAmount(1000L);
        payment.setCurrency("USD");
        payment.setCardLastFour("4242");
    }

    @Test
    @DisplayName("should approve authorization for standard cards")
    void shouldApproveStandardCards() {
        ProcessorResult result = processor.authorize(payment);

        assertTrue(result.success());
        assertNotNull(result.referenceId());
        assertTrue(result.referenceId().startsWith("sim_auth_"));
        assertNull(result.declineReason());
    }

    @Test
    @DisplayName("should decline cards ending in 0002 (insufficient funds)")
    void shouldDeclineInsufficientFunds() {
        payment.setCardLastFour("0002");

        ProcessorResult result = processor.authorize(payment);

        assertFalse(result.success());
        assertNull(result.referenceId());
        assertEquals("insufficient_funds", result.declineReason());
    }

    @Test
    @DisplayName("should decline cards ending in 0003 (processor error)")
    void shouldDeclineProcessorError() {
        payment.setCardLastFour("0003");

        ProcessorResult result = processor.authorize(payment);

        assertFalse(result.success());
        assertEquals("processor_error", result.declineReason());
    }

    @Test
    @DisplayName("should approve capture")
    void shouldApproveCapture() {
        ProcessorResult result = processor.capture(payment);

        assertTrue(result.success());
        assertTrue(result.referenceId().startsWith("sim_cap_"));
    }

    @Test
    @DisplayName("should approve refund")
    void shouldApproveRefund() {
        ProcessorResult result = processor.refund(payment);

        assertTrue(result.success());
        assertTrue(result.referenceId().startsWith("sim_ref_"));
    }

    @Test
    @DisplayName("should approve reversal")
    void shouldApproveReversal() {
        ProcessorResult result = processor.reverse(payment);

        assertTrue(result.success());
        assertTrue(result.referenceId().startsWith("sim_rev_"));
    }
}
